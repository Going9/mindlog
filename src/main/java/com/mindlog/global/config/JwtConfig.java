package com.mindlog.global.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

@Configuration
@NullMarked
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://jwaycvcwulsneslozzhf.supabase.co/auth/v1}")
    private String issuerUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        // 타임아웃 설정을 위한 RestTemplate 구성
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(5000);    // 5초
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate(factory);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(restTemplate) // 커스텀 RestTemplate 주입
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        // 2. 기본 Issuer 검증기 생성
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

        // 3. Supabase 전용 Audience 검증기 (aud: authenticated 허용)
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains("authenticated")) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "The aud claim is not 'authenticated'", null)
            );
        };

        // 4. 모든 검증기 결합
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));

        return jwtDecoder;
    }
}