package com.mindlog.global.config;

import com.mindlog.domain.auth.service.AuthHandoverService;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@Slf4j
@Component
@Order(300)
@RequiredArgsConstructor
public class BusinessWarmupRunner implements ApplicationRunner {

    private static final String WARMUP_USER_AGENT = "mindlog-warmup/1.0";
    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";
    private static final String USER_NAME_KEY = "USER_NAME";
    private static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";

    private final AuthHandoverService authHandoverService;

    @Value("${mindlog.performance.warmup-business-on-startup:true}")
    private boolean warmupEnabled;

    @Value("${mindlog.performance.warmup-profile-id:}")
    private String warmupProfileId;

    @Value("${mindlog.performance.warmup-business-paths:/diaries,/insights/emotions}")
    private String warmupBusinessPaths;

    @Value("${mindlog.performance.warmup-static-paths:}")
    private String warmupStaticPaths;

    @Value("${mindlog.performance.warmup-business-connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${mindlog.performance.warmup-business-request-timeout-ms:5000}")
    private long requestTimeoutMs;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            return;
        }
        if (!StringUtils.hasText(warmupProfileId)) {
            log.info("[BIZ-WARMUP] warmup-profile-id가 비어 있어 비즈니스 워밍업을 건너뜁니다.");
            return;
        }

        var profileId = parseProfileId(warmupProfileId.trim());
        if (profileId == null) {
            return;
        }

        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();

        var startedAt = System.currentTimeMillis();

        try {
            bootstrapAuthenticatedSession(client, profileId);
            warmupPaths(client, warmupBusinessPaths, "text/html");
            warmupPaths(client, warmupStaticPaths, "*/*");

            var elapsed = System.currentTimeMillis() - startedAt;
            log.info("[BIZ-WARMUP] 비즈니스 워밍업 완료 - profileId={}, elapsed={}ms", profileId, elapsed);
        } catch (Exception e) {
            var elapsed = System.currentTimeMillis() - startedAt;
            log.warn("[BIZ-WARMUP] 비즈니스 워밍업 실패 - profileId={}, elapsed={}ms, message={}",
                    profileId,
                    elapsed,
                    e.getMessage());
        }
    }

    private void bootstrapAuthenticatedSession(HttpClient client, UUID profileId) throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                profileId.toString(),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        var token = authHandoverService.createOneTimeToken(auth, Map.of(
                USER_NAME_KEY, "warmup-user",
                ACCESS_TOKEN_KEY, "warmup-access-token",
                REFRESH_TOKEN_KEY, "warmup-refresh-token"
        ));

        var encodedToken = UriUtils.encode(token, StandardCharsets.UTF_8);
        var uri = URI.create("http://127.0.0.1:" + serverPort + normalizeContextPath() + "/auth/exchange?token=" + encodedToken);
        var request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Accept", "text/html")
                .header("User-Agent", WARMUP_USER_AGENT)
                .build();

        var startedAt = System.currentTimeMillis();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        var elapsed = System.currentTimeMillis() - startedAt;

        if (response.statusCode() >= 400) {
            throw new IllegalStateException("auth exchange failed: status=" + response.statusCode());
        }
        log.info("[BIZ-WARMUP] 인증 세션 생성 완료 - status={}, elapsed={}ms", response.statusCode(), elapsed);
    }

    private void warmupPaths(HttpClient client, String csvPaths, String acceptHeader) {
        for (var path : parsePaths(csvPaths)) {
            warmupPath(client, path, acceptHeader);
        }
    }

    private void warmupPath(HttpClient client, String path, String acceptHeader) {
        var uri = URI.create("http://127.0.0.1:" + serverPort + normalizeContextPath() + path);
        var request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Accept", acceptHeader)
                .header("User-Agent", WARMUP_USER_AGENT)
                .build();

        var startedAt = System.currentTimeMillis();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            var elapsed = System.currentTimeMillis() - startedAt;
            var status = response.statusCode();

            if (status >= 400) {
                log.warn("[BIZ-WARMUP] 요청 실패 - path={}, status={}, elapsed={}ms", path, status, elapsed);
            } else {
                log.info("[BIZ-WARMUP] 요청 완료 - path={}, status={}, elapsed={}ms", path, status, elapsed);
            }
        } catch (Exception e) {
            var elapsed = System.currentTimeMillis() - startedAt;
            log.warn("[BIZ-WARMUP] 요청 예외 - path={}, elapsed={}ms, message={}", path, elapsed, e.getMessage());
        }
    }

    private List<String> parsePaths(String csvPaths) {
        if (!StringUtils.hasText(csvPaths)) {
            return List.of();
        }

        return Arrays.stream(csvPaths.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizePath)
                .toList();
    }

    private UUID parseProfileId(String profileId) {
        try {
            return UUID.fromString(profileId);
        } catch (IllegalArgumentException e) {
            log.warn("[BIZ-WARMUP] warmup-profile-id 형식이 잘못되어 워밍업을 건너뜁니다 - value={}", profileId);
            return null;
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }
}
