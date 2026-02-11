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
            "/favicon",
            "/css/",
            "/js/",
            "/images/",
            "/webjars/"
    );
    private static final String WARMUP_USER_AGENT_PREFIX = "mindlog-warmup/";

    @Value("${mindlog.performance.warmup-http-on-startup:false}")
    private boolean warmupHttpEnabled;

    @Value("${mindlog.performance.reject-traffic-until-warmup-complete:false}")
    private boolean rejectTrafficUntilWarmupComplete;

    private final WarmupStatus warmupStatus;

    public WarmupTrafficGateFilter(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!warmupHttpEnabled || !rejectTrafficUntilWarmupComplete) {
            return true;
        }
        if (warmupStatus.isHttpWarmupCompleted()) {
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
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "5");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Service warming up. Please retry shortly.");
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
}
