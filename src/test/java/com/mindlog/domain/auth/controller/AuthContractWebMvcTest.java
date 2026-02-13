package com.mindlog.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mindlog.domain.auth.service.AuthHandoverService;
import com.mindlog.domain.auth.service.AuthLoginService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthContractWebMvcTest {

    @Mock
    private AuthLoginService authLoginService;

    @Mock
    private AuthHandoverService authHandoverService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        var authController = new AuthController(authLoginService);
        ReflectionTestUtils.setField(authController, "supabaseUrl", "https://supabase.example.com");

        var authExchangeController = new AuthExchangeController(authHandoverService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController, authExchangeController).build();
    }

    @Test
    void socialLogin_AppSource_ContainsNativeParamsInRedirectUri() throws Exception {
        var result = mockMvc.perform(get("/auth/login/google")
                        .param("source", "app")
                        .header("Host", "www.mindlog.blog")
                        .secure(true))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var location = result.getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();
        assertThat(location).contains("https://supabase.example.com/auth/v1/authorize");
        assertThat(location).contains("provider=google");
        assertThat(location).contains("redirect_to=");
        assertThat(location).contains("source%3Dapp%26v%3D");
    }

    @Test
    void socialLogin_WebSource_DoesNotContainNativeParams() throws Exception {
        var result = mockMvc.perform(get("/auth/login/google")
                        .header("Host", "www.mindlog.blog")
                        .secure(true))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var location = result.getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();
        assertThat(location).doesNotContain("source%3Dapp%26v%3D");
    }

    @Test
    void callback_AppSource_ReturnsDeepLinkView() throws Exception {
        var encodedVerifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("verifier123".getBytes(StandardCharsets.UTF_8));
        when(authLoginService.processLoginForNativeApp("code123", "verifier123")).thenReturn("handover-token-1");

        mockMvc.perform(get("/auth/callback")
                        .param("code", "code123")
                        .param("source", "app")
                        .param("v", encodedVerifier))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/app-callback"))
                .andExpect(model().attribute("deepLinkUrl", "mindlog://auth/callback?token=handover-token-1"));
    }

    @Test
    void callback_AppSourceWithoutVerifier_RedirectsLoginWithInvalidSession() throws Exception {
        mockMvc.perform(get("/auth/callback")
                        .param("code", "code123")
                        .param("source", "app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?source=app&error=invalid_session"));
    }

    @Test
    void exchange_ValidToken_RendersExchangeCompleteView() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user-id", "token", List.of());
        var result = new AuthHandoverService.HandoverResult(
                auth,
                Map.of("USER_NAME", "tester", "ACCESS_TOKEN", "access-token"));
        when(authHandoverService.consumeToken("token-1")).thenReturn(result);

        mockMvc.perform(get("/auth/exchange").param("token", "token-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/exchange-complete"))
                .andExpect(model().attribute("redirectUrl", "/"));
    }

    @Test
    void exchange_InvalidToken_RedirectsToLogin() throws Exception {
        when(authHandoverService.consumeToken("invalid")).thenReturn(null);

        mockMvc.perform(get("/auth/exchange").param("token", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=invalid_token"));
    }
}
