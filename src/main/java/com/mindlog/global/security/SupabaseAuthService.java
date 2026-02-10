package com.mindlog.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupabaseAuthService {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @Value("${mindlog.supabase.anon-key}")
    private String supabaseAnonKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    // [신규] PKCE 흐름: 인증 코드를 토큰으로 교환
    public Map<String, Object> exchangeCodeForToken(String code, String codeVerifier) throws Exception {
        // Supabase 토큰 교환 엔드포인트
        String tokenUrl = supabaseUrl + "/auth/v1/token?grant_type=pkce";

        // 요청 바디: 코드와 검증기(Verifier)를 함께 보냄
        Map<String, String> body = Map.of(
                "auth_code", code,
                "code_verifier", codeVerifier
        );
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", supabaseAnonKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Token exchange failed: " + response.body());
        }

        return objectMapper.readValue(response.body(), Map.class);
    }

    // SupabaseAuthService.java 내부에 추가

    // [신규] Refresh Token으로 Access Token 갱신하기 (부활)
    public Map<String, Object> refreshToken(String refreshToken) throws Exception {
        String tokenUrl = supabaseUrl + "/auth/v1/token?grant_type=refresh_token";

        Map<String, String> body = Map.of("refresh_token", refreshToken);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", supabaseAnonKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("토큰 갱신 실패 (다시 로그인 필요): " + response.body());
        }

        return objectMapper.readValue(response.body(), Map.class);
    }
}
