package com.mindlog.global.config;

import lombok.RequiredArgsConstructor;
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

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // 1. CSRF 비활성화
                                .csrf(AbstractHttpConfigurer::disable)

                                // 2. 세션 정책: 필요하면 생성 (이 설정이 Spring Session과 연동됨)
                                .sessionManagement(session -> session
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
                                                .permitAll());

                return http.build();
        }
}