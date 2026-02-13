package com.mindlog.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${mindlog.security.require-https:false}")
    private boolean requireHttps;

    private final AppSourceContext appSourceContext;

    public SecurityConfig(AppSourceContext appSourceContext) {
        this.appSourceContext = appSourceContext;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/internal/warmup/run")));

        if (requireHttps) {
            http.headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts
                            .maxAgeInSeconds(31536000)
                            .includeSubDomains(true)));
        }

        http.sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/auth/**",
                                "/js/**",
                                "/css/**",
                                "/images/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/healthz",
                                "/internal/warmup/run",
                                "/favicon.ico",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect(resolveLogoutRedirectUrl(request)))
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION")
                        .clearAuthentication(true)
                        .permitAll())
                .exceptionHandling(conf -> conf
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestedWith = request.getHeader("X-Requested-With");
                            String accept = request.getHeader("Accept");
                            boolean expectsJson = accept != null && accept.contains("application/json");

                            if ("XMLHttpRequest".equals(requestedWith) || expectsJson) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                                return;
                            }

                            String loginUrl = appSourceContext.isAppSource(request)
                                    ? "/auth/login?source=app"
                                    : "/auth/login";
                            response.sendRedirect(loginUrl);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)));

        return http.build();
    }

    String resolveLogoutRedirectUrl(jakarta.servlet.http.HttpServletRequest request) {
        var isAppSource = appSourceContext.isAppSource(request);
        return isAppSource ? "/?source=app" : "/";
    }
}
