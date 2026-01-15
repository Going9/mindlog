package com.mindlog.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

    private final SecretKey key;

    public JwtTokenProvider(@Value("${SUPABASE_JWT_SECRET}") String secret) {
        // HMAC-SHA 알고리즘에 적합한 SecretKey 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰의 'sub' claim에서 Supabase User ID(UUID)를 추출합니다.
     */
    public UUID extractProfileId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * 토큰의 유효성(서명 위조, 만료 여부 등)을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // 실무에서는 로그를 남겨 상세 에러를 추적하는 것이 좋습니다.
            return false;
        }
    }
}