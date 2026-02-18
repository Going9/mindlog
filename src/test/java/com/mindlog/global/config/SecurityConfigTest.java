package com.mindlog.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private AppSourceContext appSourceContext;

    @Mock
    private HttpServletRequest request;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(appSourceContext);
    }

    @Test
    void resolveLogoutRedirectUrl_WhenSourceIsApp_ReturnsNativeHome() {
        when(appSourceContext.isAppSource(request)).thenReturn(true);

        var redirectUrl = securityConfig.resolveLogoutRedirectUrl(request);

        assertThat(redirectUrl).isEqualTo("/?source=app");
    }

    @Test
    void resolveLogoutRedirectUrl_WhenSourceIsMissing_ReturnsHome() {
        when(appSourceContext.isAppSource(request)).thenReturn(false);

        var redirectUrl = securityConfig.resolveLogoutRedirectUrl(request);

        assertThat(redirectUrl).isEqualTo("/");
    }
}
