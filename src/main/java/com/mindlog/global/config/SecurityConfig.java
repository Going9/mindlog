package com.mindlog.global.config;

import com.mindlog.global.security.JwtTokenProvider;
import com.mindlog.global.security.SupabaseJwtFilter;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
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


                // 직접 만든 필터 대신 OAuth2 Resource Server 기능 활성화
                // 이렇게 하면 jwk-set-uri를 통해 자동으로 ES256 토큰을 검증합니다.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * BearerTokenResolver를 Bean으로 등록하여 확실하게 주입되도록 합니다.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> {
            // 1. 헤더에서 토큰 확인
            String token = resolver.resolve(request);
            if (token != null) return token;

            // 2. 쿠키에서 토큰 확인 및 로그 출력 (디버깅용)
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("mindlog_access_token".equals(cookie.getName())) {
                        System.out.println("쿠키에서 토큰을 찾았습니다: " + cookie.getName());
                        return cookie.getValue();
                    }
                }
            }
            System.out.println("요청에서 토큰을 찾을 수 없습니다.");
            return null;
        };
    }

    /**
     * Spring Boot의 기본 패스워드 자동 생성을 비활성화하기 위한 Dummy 서비스입니다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("User not found"); };
    }
}