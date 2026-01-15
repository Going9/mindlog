package com.mindlog.global.logging.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Spring Security 인증/인가 이벤트를 감사 로깅하는 리스너.
 * 보안 관련 이벤트를 별도 로그 파일에 기록합니다.
 */
@Component
public class SecurityAuditListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListener.class);

    /**
     * 인증 성공 이벤트 처리
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        var authentication = event.getAuthentication();
        String principal = extractPrincipal(authentication.getPrincipal());

        // MDC에 profileId 설정 (이후 로깅에서 사용)
        MDC.put("profileId", principal);

        log.info("Authentication successful",
                kv("event", "AUTH_SUCCESS"),
                kv("principal", principal),
                kv("authorities", authentication.getAuthorities().toString())
        );
    }

    /**
     * 인증 실패 이벤트 처리
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        var authentication = event.getAuthentication();
        var exception = event.getException();
        String principal = extractPrincipal(authentication.getPrincipal());

        log.warn("Authentication failed",
                kv("event", "AUTH_FAILURE"),
                kv("principal", principal),
                kv("reason", exception.getClass().getSimpleName()),
                kv("message", exception.getMessage())
        );
    }

    /**
     * 인가 거부 이벤트 처리
     */
    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        var authentication = event.getAuthentication().get();
        String principal = extractPrincipal(authentication.getPrincipal());

        log.warn("Authorization denied",
                kv("event", "AUTHZ_DENIED"),
                kv("principal", principal),
                kv("source", event.getSource().toString())
        );
    }

    private String extractPrincipal(Object principal) {
        if (principal == null) {
            return "anonymous";
        }
        return principal.toString();
    }
}
