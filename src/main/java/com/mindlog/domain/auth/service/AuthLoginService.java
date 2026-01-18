package com.mindlog.domain.auth.service;

import com.mindlog.domain.profile.entity.Profile;
import com.mindlog.domain.profile.entity.UserRole;
import com.mindlog.domain.profile.repository.ProfileRepository;
import com.mindlog.global.security.SupabaseAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginService {

    private final SupabaseAuthService supabaseAuthService;
    private final ProfileRepository profileRepository;

    // Security 로직을 위한 도구
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * 로그인 핵심 로직: 토큰 교환 -> 프로필 동기화 -> 시큐리티 컨텍스트 저장
     */
    @Transactional
    public String processLogin(String code, String verifier, HttpServletRequest request, HttpServletResponse response) throws Exception {

        // 1. Supabase 토큰 교환
        Map<String, Object> tokenData = supabaseAuthService.exchangeCodeForToken(code, verifier);
        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");

        // 2. 유저 정보 파싱
        Map<String, Object> userMap = (Map<String, Object>) tokenData.get("user");
        String userIdStr = (String) userMap.get("id");
        String email = (String) userMap.get("email");
        UUID profileId = UUID.fromString(userIdStr);

        // 3. [핵심] 프로필 동기화 (DB에 없으면 생성)
        if (!profileRepository.existsById(profileId)) {
            Map<String, Object> userMeta = (Map<String, Object>) userMap.get("user_metadata");

            // 기본값 설정
            String name = email.split("@")[0]; // 'name' (화면 표시 이름)
            String avatar = null;              // 'avatar' (이미지)

            if (userMeta != null) {
                // name 추출 우선순위: full_name -> name -> 이메일 앞부분
                if (userMeta.get("full_name") != null) name = (String) userMeta.get("full_name");
                else if (userMeta.get("name") != null) name = (String) userMeta.get("name");

                // avatar 추출 우선순위: avatar_url -> picture
                if (userMeta.get("avatar_url") != null) avatar = (String) userMeta.get("avatar_url");
                else if (userMeta.get("picture") != null) avatar = (String) userMeta.get("picture");
            }

            // [중요] userName 생성 로직 (Unique 제약조건 준수)
            // 트리거 로직과 동일하게: 이메일아이디 + '_' + UUID앞8자리
            String userName = email.split("@")[0] + "_" + userIdStr.substring(0, 8);

            Profile newProfile = Profile.builder()
                    .id(profileId)
                    .email(email)
                    .name(name)           // 수정됨: nickname -> name
                    .userName(userName)   // 추가됨: userName (필수)
                    .avatar(avatar)       // 수정됨: profileImageUrl -> avatar
                    .role(UserRole.USER)
                    .build();

            profileRepository.save(newProfile);
            log.info("신규 유저 프로필 동기화 완료: {} ({})", name, userName);
        }

        // 4. Spring Security 인증 처리 (수동 로그인)
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userIdStr, // Principal
                accessToken, // Credentials
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // 5. 세션에 토큰 보관
        HttpSession session = request.getSession();
        session.setAttribute("ACCESS_TOKEN", accessToken);
        if (refreshToken != null) {
            session.setAttribute("REFRESH_TOKEN", refreshToken);
        }

        return userIdStr;
    }
}