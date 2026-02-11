package com.mindlog.global.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class HttpWarmupRunner implements ApplicationRunner {

    @Value("${mindlog.performance.warmup-http-on-startup:false}")
    private boolean warmupHttpEnabled;

    @Value("${mindlog.performance.warmup-http-paths:/}")
    private String warmupHttpPaths;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final WarmupStatus warmupStatus;

    public HttpWarmupRunner(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupHttpEnabled) {
            warmupStatus.markHttpWarmupCompleted();
            return;
        }

        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        try {
            Arrays.stream(warmupHttpPaths.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(path -> warmupPath(client, normalizePath(path)));
        } finally {
            warmupStatus.markHttpWarmupCompleted();
        }
    }

    private void warmupPath(HttpClient client, String path) {
        var uri = URI.create("http://127.0.0.1:" + serverPort + normalizeContextPath() + path);
        var request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "text/html")
                .header("User-Agent", "mindlog-warmup/1.0")
                .build();

        var startedAt = System.currentTimeMillis();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            var elapsed = System.currentTimeMillis() - startedAt;
            var status = response.statusCode();

            if (status >= 500) {
                log.warn("[WARMUP] HTTP 워밍업 실패 - path={}, status={}, elapsed={}ms", path, status, elapsed);
            } else {
                log.info("[WARMUP] HTTP 워밍업 완료 - path={}, status={}, elapsed={}ms", path, status, elapsed);
            }
        } catch (Exception e) {
            var elapsed = System.currentTimeMillis() - startedAt;
            log.warn("[WARMUP] HTTP 워밍업 예외 - path={}, elapsed={}ms, message={}", path, elapsed, e.getMessage());
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeContextPath() {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }
}
