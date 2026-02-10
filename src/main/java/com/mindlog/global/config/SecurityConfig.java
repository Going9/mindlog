package com.mindlog.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        @Value("${mindlog.security.require-https:false}")
        private boolean requireHttps;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                // 1. CSRF 비활성화
                http.csrf(AbstractHttpConfigurer::disable);

                // 1-1. 운영 환경에서만 HTTPS 강제/HSTS 적용
                if (requireHttps) {
                        http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
                        http.headers(headers -> headers
                                        .httpStrictTransportSecurity(hsts -> hsts
                                                        .maxAgeInSeconds(31536000)
                                                        .includeSubDomains(true)));
                }

                // 2. 세션 정책: 필요하면 생성 (이 설정이 Spring Session과 연동됨)
                http.sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .maximumSessions(1) // (선택) 중복 로그인 방지
                )

                                // 3. 페이지별 접근 권한 설정
                                .authorizeHttpRequests(auth -> auth
                                                // 정적 파일 및 로그인 관련 경로는 모두 허용
                                                .requestMatchers("/", "/auth/**", "/js/**", "/css/**", "/images/**",
                                                                "/favicon.ico", "/error")
                                                .permitAll()
                                                // 그 외 모든 페이지(일기장 등)는 로그인해야 접근 가능
                                                .anyRequest().authenticated())

                                // 4. 로그아웃 설정
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/") // 로그아웃하면 홈 화면으로 이동
                                                .invalidateHttpSession(true) // 서버 세션 무효화
                                                .deleteCookies("SESSION") // 우리가 설정한 쿠키 이름 삭제
                                                .clearAuthentication(true) // 인증 정보 확실히 삭제
                                                .permitAll())

                                // 5. 예외 처리 설정
                                .exceptionHandling(conf -> conf
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // AJAX 또는 Turbo 요청인지 확인
                                                        String requestedWith = request.getHeader("X-Requested-With");
                                                        String accept = request.getHeader("Accept");
                                                        boolean isTurbo = accept != null
                                                                        && (accept.contains(
                                                                                        "text/vnd.turbo-stream.html")
                                                                                        || accept.contains(
                                                                                                        "application/json"));

                                                        if ("XMLHttpRequest".equals(requestedWith) || isTurbo) {
                                                                // JS에서 처리할 수 있도록 401 반환
                                                                response.sendError(
                                                                                jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                                        } else {
                                                                // 일반 브라우저 요청은 로그인 페이지로 이동
                                                                response.sendRedirect("/auth/login");
                                                        }
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        // 권한 부족 시 403 에러 처리 (Spring Boot가 error/4xx.html을 자동으로 찾음)
                                                        response.sendError(
                                                                        jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                                                }));

                return http.build();
        }
}
