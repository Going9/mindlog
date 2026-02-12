package com.mindlog.global.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(500)
public class SupabaseWarmupRunner implements ApplicationRunner {

    @Value("${mindlog.performance.warmup-supabase-on-startup:false}")
    private boolean warmupEnabled;

    @Value("${mindlog.performance.warmup-supabase-connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${mindlog.performance.warmup-supabase-request-timeout-ms:5000}")
    private long requestTimeoutMs;

    @Value("${mindlog.performance.warmup-supabase-path:/auth/v1/settings}")
    private String warmupPath;

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @Value("${mindlog.supabase.anon-key}")
    private String supabaseAnonKey;

    private final WarmupStatus warmupStatus;

    public SupabaseWarmupRunner(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            warmupStatus.markSupabaseWarmupCompleted();
            return;
        }

        executeWarmup();
    }

    public void warmupNow() {
        executeWarmup();
    }

    private void executeWarmup() {
        long startedAt = System.currentTimeMillis();
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(buildWarmupUrl()))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("apikey", supabaseAnonKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsed = System.currentTimeMillis() - startedAt;
            int status = response.statusCode();

            if (status >= 500) {
                log.warn("[SUPABASE] 워밍업 실패 - status={}, elapsed={}ms", status, elapsed);
            } else {
                log.info("[SUPABASE] 워밍업 완료 - status={}, elapsed={}ms", status, elapsed);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startedAt;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("[SUPABASE] 워밍업 예외 - elapsed={}ms, exception={}, message={}",
                    elapsed,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            log.debug("[SUPABASE] 워밍업 예외 상세", e);
        } finally {
            warmupStatus.markSupabaseWarmupCompleted();
        }
    }

    private String buildWarmupUrl() {
        String base = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String path = warmupPath.startsWith("/") ? warmupPath : "/" + warmupPath;
        return base + path;
    }
}
