package com.mindlog.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
public class AuthHandoverService {

    // 토큰과 함께 필요한 데이터를 저장하는 내부 클래스
    private record HandoverData(
            Authentication authentication,
            Map<String, Object> sessionAttributes,
            Instant createdAt) {
    }

    // 운영 환경에서는 Redis로 대체 권장 (TTL 1분 설정)
    private final Map<String, HandoverData> tokenStore = new ConcurrentHashMap<>();

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
        tokenStore.put(token, new HandoverData(authentication, sessionAttributes, Instant.now()));
        log.info("Handover 토큰 생성: {} (사용자: {})", token.substring(0, 8) + "...", authentication.getName());

        // 간단한 만료된 토큰 정리 (백그라운드 스레드 대신 호출 시점에 정리)
        cleanupExpiredTokens();

        return token;
    }

    /**
     * 토큰을 소비하고 인증 정보를 반환합니다 (일회용).
     * 
     * @param token 일회용 토큰
     * @return 인증 정보와 세션 속성을 담은 맵, 유효하지 않으면 null
     */
    public HandoverResult consumeToken(String token) {
        var data = tokenStore.remove(token);

        if (data == null) {
            log.warn("Handover 토큰 조회 실패: 존재하지 않는 토큰");
            return null;
        }

        // 만료 확인
        if (Instant.now().isAfter(data.createdAt().plusSeconds(TOKEN_TTL_SECONDS))) {
            log.warn("Handover 토큰 만료: {} (생성: {})", token.substring(0, 8) + "...", data.createdAt());
            return null;
        }

        log.info("Handover 토큰 교환 성공: {} (사용자: {})", token.substring(0, 8) + "...", data.authentication().getName());
        return new HandoverResult(data.authentication(), data.sessionAttributes());
    }

    /**
     * 만료된 토큰 정리
     */
    private void cleanupExpiredTokens() {
        var now = Instant.now();
        tokenStore.entrySet()
                .removeIf(entry -> now.isAfter(entry.getValue().createdAt().plusSeconds(TOKEN_TTL_SECONDS * 2)));
    }

    /**
     * 토큰 교환 결과를 담는 레코드
     */
    public record HandoverResult(
            Authentication authentication,
            Map<String, Object> sessionAttributes) {
    }
}
