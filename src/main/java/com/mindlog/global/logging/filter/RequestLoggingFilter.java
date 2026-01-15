package com.mindlog.global.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * HTTP 요청/응답을 로깅하는 필터.
 * 모든 요청에 대해 requestId를 생성하고 MDC에 설정하여 추적 가능하게 합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_CLIENT_IP = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 요청 ID 생성 또는 헤더에서 추출
        String requestId = extractOrGenerateRequestId(request);
        String clientIp = extractClientIp(request);

        // MDC에 컨텍스트 정보 설정
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, clientIp);

        // 응답 헤더에 requestId 추가 (클라이언트 추적용)
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            // 요청 시작 로깅
            logRequestStart(request, requestId, clientIp);

            // 다음 필터 실행
            filterChain.doFilter(request, response);

        } finally {
            // 요청 완료 로깅
            long duration = System.currentTimeMillis() - startTime;
            logRequestComplete(request, response, duration);

            // MDC 정리
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_CLIENT_IP);
        }
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }

    private String extractClientIp(HttpServletRequest request) {
        // 프록시/로드밸런서 뒤에 있을 경우 실제 클라이언트 IP 추출
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void logRequestStart(HttpServletRequest request, String requestId, String clientIp) {
        if (shouldSkipLogging(request)) {
            return;
        }

        log.info("HTTP Request started",
                kv("method", request.getMethod()),
                kv("uri", request.getRequestURI()),
                kv("queryString", maskSensitiveParams(request.getQueryString())),
                kv("userAgent", request.getHeader("User-Agent"))
        );
    }

    private void logRequestComplete(HttpServletRequest request, HttpServletResponse response, long duration) {
        if (shouldSkipLogging(request)) {
            return;
        }

        int status = response.getStatus();
        String message = "HTTP Request completed";

        if (status >= 500) {
            log.error(message,
                    kv("method", request.getMethod()),
                    kv("uri", request.getRequestURI()),
                    kv("status", status),
                    kv("duration", duration)
            );
        } else if (status >= 400) {
            log.warn(message,
                    kv("method", request.getMethod()),
                    kv("uri", request.getRequestURI()),
                    kv("status", status),
                    kv("duration", duration)
            );
        } else {
            log.info(message,
                    kv("method", request.getMethod()),
                    kv("uri", request.getRequestURI()),
                    kv("status", status),
                    kv("duration", duration)
            );
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 헬스체크 등 불필요한 로깅 제외
        return uri.equals("/health") || uri.equals("/actuator/health") || uri.startsWith("/actuator/");
    }

    private String maskSensitiveParams(String queryString) {
        if (queryString == null) {
            return null;
        }
        // 민감한 파라미터 마스킹
        return queryString.replaceAll("(password|token|secret|key)=[^&]*", "$1=***");
    }
}
