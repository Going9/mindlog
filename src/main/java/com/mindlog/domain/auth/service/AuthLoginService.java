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

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Transactional
    public String processLogin(String code, String verifier, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> tokenData = supabaseAuthService.exchangeCodeForToken(code, verifier);
        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");

        Map<String, Object> userMap = (Map<String, Object>) tokenData.get("user");
        String userIdStr = (String) userMap.get("id");
        String email = (String) userMap.get("email");
        UUID profileId = UUID.fromString(userIdStr);

        Map<String, Object> userMeta = (Map<String, Object>) userMap.get("user_metadata");
        String name = email.split("@")[0];
        String avatar = null;

        if (userMeta != null) {
            if (userMeta.get("full_name") != null) name = (String) userMeta.get("full_name");
            else if (userMeta.get("name") != null) name = (String) userMeta.get("name");

            if (userMeta.get("avatar_url") != null) avatar = (String) userMeta.get("avatar_url");
            else if (userMeta.get("picture") != null) avatar = (String) userMeta.get("picture");
        }

        if (!profileRepository.existsById(profileId)) {
            String userName = email.split("@")[0] + "_" + userIdStr.substring(0, 8);

            Profile newProfile = Profile.builder()
                    .id(profileId)
                    .email(email)
                    .name(name)
                    .userName(userName)
                    .avatar(avatar)
                    .role(UserRole.USER)
                    .build();

            profileRepository.save(newProfile);
            log.info("신규 유저 프로필 동기화 완료: {} ({})", name, userName);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userIdStr,
                accessToken,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession();
        session.setAttribute("ACCESS_TOKEN", accessToken);
        session.setAttribute("USER_NAME", name);
        if (refreshToken != null) {
            session.setAttribute("REFRESH_TOKEN", refreshToken);
        }

        return userIdStr;
    }
}
