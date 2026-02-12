package com.mindlog.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class WarmupTrafficGateFilter extends OncePerRequestFilter {

    private static final Set<String> PASS_PREFIXES = Set.of(
            "/actuator",
            "/health",
            "/internal/warmup/run",
            "/favicon",
            "/css/",
            "/js/",
            "/images/",
            "/webjars/"
    );
    private static final String WARMUP_USER_AGENT_PREFIX = "mindlog-warmup/";

    @Value("${mindlog.performance.reject-traffic-until-warmup-complete:false}")
    private boolean rejectTrafficUntilWarmupComplete;

    @Value("${mindlog.performance.warmup-expected-wait-seconds:90}")
    private int expectedWaitSeconds;

    private final WarmupStatus warmupStatus;

    public WarmupTrafficGateFilter(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!rejectTrafficUntilWarmupComplete) {
            return true;
        }
        if (warmupStatus.isStartupWarmupCompleted()) {
            return true;
        }

        var userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.startsWith(WARMUP_USER_AGENT_PREFIX)) {
            return true;
        }

        var uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }
        for (var prefix : PASS_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return hasStaticExtension(uri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var waitSeconds = Math.max(5, expectedWaitSeconds);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(buildMaintenancePage(waitSeconds));
    }

    private boolean hasStaticExtension(String uri) {
        var normalized = uri.toLowerCase();
        return normalized.endsWith(".css")
                || normalized.endsWith(".js")
                || normalized.endsWith(".map")
                || normalized.endsWith(".png")
                || normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg")
                || normalized.endsWith(".gif")
                || normalized.endsWith(".svg")
                || normalized.endsWith(".ico")
                || normalized.endsWith(".woff")
                || normalized.endsWith(".woff2")
                || normalized.endsWith(".ttf");
    }

    private String buildMaintenancePage(int waitSeconds) {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Mindlog 업데이트 중</title>
                  <meta http-equiv="refresh" content="30">
                  <style>
                    :root { color-scheme: light; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      background: linear-gradient(180deg, #f8fafc 0%%, #eef2ff 100%%);
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                      color: #0f172a;
                    }
                    .card {
                      width: min(560px, calc(100%% - 32px));
                      background: #fff;
                      border: 1px solid #e2e8f0;
                      border-radius: 16px;
                      padding: 28px 24px;
                      box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
                    }
                    h1 { margin: 0 0 8px; font-size: 24px; }
                    p { margin: 8px 0; line-height: 1.6; color: #334155; }
                    .wait {
                      margin-top: 14px;
                      display: inline-block;
                      background: #e2e8f0;
                      color: #0f172a;
                      border-radius: 999px;
                      padding: 6px 12px;
                      font-size: 14px;
                      font-weight: 600;
                    }
                  </style>
                </head>
                <body>
                  <main class="card">
                    <h1>서비스 업데이트 중입니다</h1>
                    <p>더 안정적인 서비스를 위해 내부 작업을 진행하고 있습니다.</p>
                    <p>잠시 후 자동으로 다시 시도해 주세요.</p>
                    <div class="wait">예상 대기 시간: 약 %d초</div>
                  </main>
                </body>
                </html>
                """.formatted(waitSeconds);
    }
}
