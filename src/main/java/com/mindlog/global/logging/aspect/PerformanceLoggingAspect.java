package com.mindlog.global.logging.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Controller 및 Service 레이어의 메서드 실행 시간을 측정하는 AOP Aspect.
 * 임계값을 초과하는 느린 메서드는 WARN 레벨로 로깅합니다.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceLoggingAspect.class);

    @Value("${mindlog.logging.performance.enabled:true}")
    private boolean enabled;

    @Value("${mindlog.logging.performance.controller-slow-threshold-ms:1200}")
    private long controllerSlowThresholdMs;

    @Value("${mindlog.logging.performance.service-slow-threshold-ms:1500}")
    private long serviceSlowThresholdMs;

    @Value("${mindlog.logging.performance.log-success-debug:false}")
    private boolean logSuccessDebug;

    @Pointcut("within(com.mindlog..*) && (within(@org.springframework.web.bind.annotation.RestController *) || " +
              "within(@org.springframework.stereotype.Controller *))")
    public void controllerMethods() {
    }

    @Pointcut("within(com.mindlog..*) && within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {
    }

    @Around("controllerMethods()")
    public Object logControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint, "controller", controllerSlowThresholdMs);
    }

    @Around("serviceMethods()")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint, "service", serviceSlowThresholdMs);
    }

    private Object measureAndLog(ProceedingJoinPoint joinPoint, String layer, long slowThresholdMs) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        long startTime = System.nanoTime();
        boolean success = true;
        String errorType = null;

        try {
            return joinPoint.proceed();
        } catch (Error err) {
            throw err;
        } catch (Throwable ex) {
            success = false;
            errorType = ex.getClass().getSimpleName();
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logExecution(fullMethodName, layer, durationMs, slowThresholdMs, success, errorType);
        }
    }

    private void logExecution(String fullMethodName, String layer, long durationMs, long slowThresholdMs,
                              boolean success, String errorType) {
        if (!success) {
            log.error("Method execution failed: {} ({}ms, layer={})",
                    fullMethodName, durationMs, layer,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("durationMs", durationMs),
                    kv("success", false),
                    kv("errorType", errorType)
            );
            return;
        }

        if (durationMs >= slowThresholdMs) {
            log.warn("Slow method execution: {} took {}ms (threshold={}ms, layer={})",
                    fullMethodName, durationMs, slowThresholdMs, layer,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("durationMs", durationMs),
                    kv("thresholdMs", slowThresholdMs),
                    kv("success", true)
            );
            return;
        }

        if (logSuccessDebug && log.isDebugEnabled()) {
            log.debug("Method execution completed: {} ({}ms, layer={})",
                    fullMethodName, durationMs, layer,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("durationMs", durationMs),
                    kv("success", true)
            );
        }
    }
}
