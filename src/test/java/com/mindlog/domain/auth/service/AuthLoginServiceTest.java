package com.mindlog.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindlog.domain.profile.entity.Profile;
import com.mindlog.domain.profile.entity.UserRole;
import com.mindlog.domain.profile.repository.ProfileRepository;
import com.mindlog.global.security.SupabaseAuthService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthLoginServiceTest {

    @Mock
    private SupabaseAuthService supabaseAuthService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AuthHandoverService authHandoverService;

    @InjectMocks
    private AuthLoginService authLoginService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("웹 로그인 처리 - 세션과 SecurityContext를 저장한다")
    void processLogin_SavesSessionAndContext() throws Exception {
        var userId = UUID.randomUUID().toString();
        var profileId = UUID.fromString(userId);
        when(supabaseAuthService.exchangeCodeForToken("code-1", "verifier-1")).thenReturn(tokenData(
                userId,
                "tester@example.com",
                "access-token-1",
                "refresh-token-1",
                "테스터",
                "https://avatar.test"));
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        var result = authLoginService.processLogin("code-1", "verifier-1", request, response);

        assertThat(result).isEqualTo(userId);
        assertThat(request.getSession().getAttribute("ACCESS_TOKEN")).isEqualTo("access-token-1");
        assertThat(request.getSession().getAttribute("USER_NAME")).isEqualTo("테스터");
        assertThat(request.getSession().getAttribute("REFRESH_TOKEN")).isEqualTo("refresh-token-1");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(userId);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    @DisplayName("네이티브 로그인 처리 - handover 토큰 생성 시 세션 속성을 전달한다")
    void processLoginForNativeApp_CreatesHandoverToken() throws Exception {
        var userId = UUID.randomUUID().toString();
        var profileId = UUID.fromString(userId);
        when(supabaseAuthService.exchangeCodeForToken("code-2", "verifier-2")).thenReturn(tokenData(
                userId,
                "native@example.com",
                "access-token-2",
                null,
                "네이티브유저",
                null));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(Profile.builder()
                .id(profileId)
                .email("native@example.com")
                .name("네이티브유저")
                .userName("native_user")
                .role(UserRole.USER)
                .build()));
        when(authHandoverService.createOneTimeToken(any(), anyMap())).thenReturn("handover-token-1");

        var result = authLoginService.processLoginForNativeApp("code-2", "verifier-2");

        assertThat(result).isEqualTo("handover-token-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> attrsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(authHandoverService).createOneTimeToken(any(), attrsCaptor.capture());
        var attrs = attrsCaptor.getValue();
        assertThat(attrs.get("ACCESS_TOKEN")).isEqualTo("access-token-2");
        assertThat(attrs.get("USER_NAME")).isEqualTo("네이티브유저");
        assertThat(attrs).doesNotContainKey("REFRESH_TOKEN");
    }

    private Map<String, Object> tokenData(
            String userId,
            String email,
            String accessToken,
            String refreshToken,
            String fullName,
            String avatarUrl) {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("access_token", accessToken);
        if (refreshToken != null) {
            tokenData.put("refresh_token", refreshToken);
        }

        Map<String, Object> userMetadata = new HashMap<>();
        if (fullName != null) {
            userMetadata.put("full_name", fullName);
        }
        if (avatarUrl != null) {
            userMetadata.put("avatar_url", avatarUrl);
        }

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("email", email);
        user.put("user_metadata", userMetadata);
        tokenData.put("user", user);
        return tokenData;
    }
}
