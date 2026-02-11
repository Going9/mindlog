package com.mindlog.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 네이티브 앱(Android)의 Custom Tab ↔ WebView 간 인증 인계(Handover)를 담당하는 서비스.
 * 
 * Custom Tab에서 로그인 완료 후 일회용 토큰을 생성하고,
 * WebView가 해당 토큰을 교환하여 세션을 획득합니다.
 * 
 * 운영 환경(다중 서버)에서는 Redis로 대체하는 것을 권장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthHandoverService {
    private static final String TOKEN_KEY_PREFIX = "auth:handover:";
    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);

    // 토큰과 함께 필요한 데이터를 저장하는 내부 클래스
    private record HandoverData(
            Authentication authentication,
            Map<String, Object> sessionAttributes,
            Instant createdAt) {
    }

    private record StoredHandoverData(
            String principal,
            List<String> authorities,
            Map<String, Object> sessionAttributes,
            Instant createdAt
    ) {
    }

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, HandoverData> fallbackTokenStore = new ConcurrentHashMap<>();

    // 토큰 유효 시간: 60초
    private static final long TOKEN_TTL_SECONDS = 60;

    /**
     * 일회용 토큰을 생성합니다.
     * 
     * @param authentication    인증 정보
     * @param sessionAttributes 세션에 저장할 추가 속성 (ACCESS_TOKEN, USER_NAME 등)
     * @return 생성된 일회용 토큰
     */
    public String createOneTimeToken(Authentication authentication, Map<String, Object> sessionAttributes) {
        var token = UUID.randomUUID().toString();
        var now = Instant.now();
        var stored = new StoredHandoverData(
                String.valueOf(authentication.getPrincipal()),
                authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList(),
                sessionAttributes,
                now
        );

        if (!saveToRedis(token, stored)) {
            fallbackTokenStore.put(token, new HandoverData(authentication, sessionAttributes, now));
            cleanupExpiredFallbackTokens();
        }

        log.info("Handover 토큰 생성: {} (사용자: {})", token.substring(0, 8) + "...", authentication.getName());

        return token;
    }

    /**
     * 토큰을 소비하고 인증 정보를 반환합니다 (일회용).
     * 
     * @param token 일회용 토큰
     * @return 인증 정보와 세션 속성을 담은 맵, 유효하지 않으면 null
     */
    public HandoverResult consumeToken(String token) {
        var fromRedis = consumeFromRedis(token);
        if (fromRedis != null) {
            if (Instant.now().isAfter(fromRedis.createdAt().plusSeconds(TOKEN_TTL_SECONDS))) {
                log.warn("Handover 토큰 만료: {} (생성: {})", token.substring(0, 8) + "...", fromRedis.createdAt());
                return null;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    fromRedis.principal(),
                    "N/A",
                    fromRedis.authorities().stream().map(SimpleGrantedAuthority::new).toList()
            );
            log.info("Handover 토큰 교환 성공: {} (사용자: {})", token.substring(0, 8) + "...", authentication.getName());
            return new HandoverResult(authentication, fromRedis.sessionAttributes());
        }

        var fallback = fallbackTokenStore.remove(token);
        if (fallback == null) {
            log.warn("Handover 토큰 조회 실패: 존재하지 않는 토큰");
            return null;
        }

        if (Instant.now().isAfter(fallback.createdAt().plusSeconds(TOKEN_TTL_SECONDS))) {
            log.warn("Handover 토큰 만료: {} (생성: {})", token.substring(0, 8) + "...", fallback.createdAt());
            return null;
        }

        log.info("Handover 토큰 교환 성공: {} (사용자: {})", token.substring(0, 8) + "...", fallback.authentication().getName());
        return new HandoverResult(fallback.authentication(), fallback.sessionAttributes());
    }

    /**
     * 만료된 토큰 정리
     */
    private void cleanupExpiredFallbackTokens() {
        var now = Instant.now();
        fallbackTokenStore.entrySet()
                .removeIf(entry -> now.isAfter(entry.getValue().createdAt().plusSeconds(TOKEN_TTL_SECONDS * 2)));
    }

    private boolean saveToRedis(String token, StoredHandoverData payload) {
        try {
            var serialized = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, serialized, TOKEN_TTL);
            return true;
        } catch (Exception e) {
            log.warn("Handover 토큰 Redis 저장 실패: {}", e.getMessage());
            return false;
        }
    }

    private StoredHandoverData consumeFromRedis(String token) {
        try {
            var key = TOKEN_KEY_PREFIX + token;
            var payload = redisTemplate.opsForValue().getAndDelete(key);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return objectMapper.readValue(payload, StoredHandoverData.class);
        } catch (UnsupportedOperationException unsupportedOperationException) {
            try {
                var key = TOKEN_KEY_PREFIX + token;
                var payload = redisTemplate.opsForValue().get(key);
                if (payload == null || payload.isBlank()) {
                    return null;
                }
                redisTemplate.delete(key);
                return objectMapper.readValue(payload, StoredHandoverData.class);
            } catch (Exception e) {
                log.warn("Handover 토큰 Redis 조회 실패: {}", e.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.warn("Handover 토큰 Redis 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 교환 결과를 담는 레코드
     */
    public record HandoverResult(
            Authentication authentication,
            Map<String, Object> sessionAttributes) {
    }
}
