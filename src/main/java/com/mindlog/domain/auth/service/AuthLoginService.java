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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginService {
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";
    private static final String USER_NAME_KEY = "USER_NAME";
    private static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";

    private final SupabaseAuthService supabaseAuthService;
    private final ProfileRepository profileRepository;
    private final AuthHandoverService authHandoverService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Transactional
    public String processLogin(String code, String verifier, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        var loginContext = authenticateWithSupabase(code, verifier);
        var auth = createAuthentication(loginContext.userId(), loginContext.accessToken());

        saveSecurityContext(auth, request, response);
        storeSessionAttributes(request.getSession(), loginContext.metadata().name(), loginContext.accessToken(), loginContext.refreshToken());
        return loginContext.userId();
    }

    /**
     * 네이티브 앱(Android)에서의 로그인 처리.
     * 
     * Custom Tab에서 OAuth 인증 완료 후, 일회용 토큰을 생성하여 반환합니다.
     * WebView가 이 토큰을 /auth/exchange 엔드포인트에 제출하면 세션이 생성됩니다.
     * 
     * @param code     OAuth authorization code
     * @param verifier PKCE code verifier
     * @return 일회용 핸드오버 토큰
     */
    @Transactional
    public String processLoginForNativeApp(String code, String verifier) throws Exception {
        var loginContext = authenticateWithSupabase(code, verifier);
        var auth = createAuthentication(loginContext.userId(), loginContext.accessToken());
        var sessionAttributes = createSessionAttributes(loginContext.metadata().name(), loginContext.accessToken(), loginContext.refreshToken());
        return authHandoverService.createOneTimeToken(auth, sessionAttributes);
    }

    /**
     * 사용자 메타데이터 추출
     * OAuth 제공자로부터 받은 사용자 정보에서 name과 avatar를 추출합니다.
     */
    private UserMetadata extractUserMetadata(String email, Map<String, Object> userMap) {
        Map<String, Object> userMeta = (Map<String, Object>) userMap.get("user_metadata");
        String name = email.split("@")[0];
        String avatar = null;

        if (userMeta != null) {
            if (userMeta.get("full_name") != null)
                name = (String) userMeta.get("full_name");
            else if (userMeta.get("name") != null)
                name = (String) userMeta.get("name");

            if (userMeta.get("avatar_url") != null)
                avatar = (String) userMeta.get("avatar_url");
            else if (userMeta.get("picture") != null)
                avatar = (String) userMeta.get("picture");
        }

        return new UserMetadata(name, avatar);
    }

    private LoginContext authenticateWithSupabase(String code, String verifier) throws Exception {
        Map<String, Object> tokenData = supabaseAuthService.exchangeCodeForToken(code, verifier);
        var accessToken = (String) tokenData.get("access_token");
        var refreshToken = (String) tokenData.get("refresh_token");

        Map<String, Object> userMap = (Map<String, Object>) tokenData.get("user");
        var userId = (String) userMap.get("id");
        var email = (String) userMap.get("email");
        var profileId = UUID.fromString(userId);
        var metadata = extractUserMetadata(email, userMap);

        syncUserProfile(profileId, email, metadata.name(), metadata.avatar());
        return new LoginContext(userId, accessToken, refreshToken, metadata);
    }

    private UsernamePasswordAuthenticationToken createAuthentication(String userId, String accessToken) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                accessToken,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    private void saveSecurityContext(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void storeSessionAttributes(HttpSession session, String userName, String accessToken, String refreshToken) {
        var sessionAttributes = createSessionAttributes(userName, accessToken, refreshToken);
        sessionAttributes.forEach(session::setAttribute);
    }

    private Map<String, Object> createSessionAttributes(String userName, String accessToken, String refreshToken) {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(ACCESS_TOKEN_KEY, accessToken);
        sessionAttributes.put(USER_NAME_KEY, userName);
        if (refreshToken != null) {
            sessionAttributes.put(REFRESH_TOKEN_KEY, refreshToken);
        }
        return sessionAttributes;
    }

    /**
     * 사용자 프로필 동기화
     * 프로필이 존재하지 않으면 새로 생성합니다.
     */
    private void syncUserProfile(UUID profileId, String email, String name, String avatar) {
        var existingProfile = profileRepository.findById(profileId);
        if (existingProfile.isPresent()) {
            return;
        }

        String userName = email.split("@")[0] + "_" + profileId.toString().substring(0, 8);

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

    /**
     * 사용자 메타데이터를 담는 Record
     */
    private record UserMetadata(String name, String avatar) {
    }

    private record LoginContext(
            String userId,
            String accessToken,
            String refreshToken,
            UserMetadata metadata) {
    }
}
