package com.mindlog.global.config;

import com.mindlog.global.security.JwtTokenProvider;
import com.mindlog.global.security.SupabaseJwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless API 서버이므로 불필요한 보안 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가(Authorization) 정책 설정
                .authorizeHttpRequests(auth -> auth
                        // 홈 페이지
                        .requestMatchers("/").permitAll()
                        // 인증 페이지 (로그인, 콜백, 로그아웃)
                        .requestMatchers("/auth/**").permitAll()
                        // 정적 리소스 (JS, CSS, 이미지 등)
                        .requestMatchers("/js/**", "/css/**", "/images/**", "/favicon.ico").permitAll()
                        // 공개 API 및 헬스체크
                        .requestMatchers("/api/public/**", "/health").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 인증 필터 이전에 배치
                .addFilterBefore(new SupabaseJwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Spring Boot의 기본 패스워드 자동 생성을 비활성화하기 위한 Dummy 서비스입니다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("User not found"); };
    }
}