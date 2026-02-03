package com.mindlog.global.logging.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * 느린 메서드로 간주하는 임계값 (밀리초)
     */
    private static final long SLOW_EXECUTION_THRESHOLD_MS = 500;

    /**
     * Controller 레이어 포인트컷
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || " +
              "within(@org.springframework.stereotype.Controller *)")
    public void controllerMethods() {}

    /**
     * Service 레이어 포인트컷
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    /**
     * Controller 메서드 실행 시간 측정
     */
    @Around("controllerMethods()")
    public Object logControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint, "controller");
    }

    /**
     * Service 메서드 실행 시간 측정
     */
    @Around("serviceMethods()")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint, "service");
    }

    /**
     * 메서드 실행 시간 측정 및 로깅
     * ProceedingJoinPoint: 실행 대상 메서드 정보
     * joinPoint.proceed(): 실제 메서드 실행
     * layer: Controller 또는 Service
     * duration: 메서드 실행 시간
     * success: 메서드 실행 성공 여부
     * errorType: 메서드 실행 오류 타입
     * fullMethodName: 메서드 전체 이름 (클래스명.메서드명)
     * logExecution: 메서드 실행 로깅
     */
    private Object measureAndLog(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
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
            long duration = System.currentTimeMillis() - startTime;
            logExecution(className, methodName, layer, duration, success, errorType);
        }
    }

    /**
     * 메서드 실행 로깅
     * className: 메서드가 속한 클래스명
     * methodName: 메서드명
     * layer: Controller 또는 Service
     * duration: 메서드 실행 시간
     * success: 메서드 실행 성공 여부
     * errorType: 메서드 실행 오류 타입
     * fullMethodName: 메서드 전체 이름 (클래스명.메서드명)
     * logExecution: 메서드 실행 로깅
     * log.warn: 메서드 실행 실패 로깅
     * log.warn: 느린 메서드 실행 감지 로깅
     * log.debug: 메서드 실행 완료 로깅
     * kv: 로깅 인수 구조화
     * duration: 메서드 실행 시간
     * success: 메서드 실행 성공 여부
     * errorType: 메서드 실행 오류 타입
     * fullMethodName: 메서드 전체 이름 (클래스명.메서드명)
     */
    private void logExecution(String className, String methodName, String layer,
                              long duration, boolean success, String errorType) {
        String fullMethodName = className + "." + methodName;

        if (!success) {
            log.warn("Method execution failed: {} ({}ms) - Error: {}", 
                    fullMethodName, duration, errorType,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("duration", duration),
                    kv("success", false),
                    kv("errorType", errorType)
            );
        } else if (duration >= SLOW_EXECUTION_THRESHOLD_MS) {
            log.warn("Slow method execution: {} took {}ms (Threshold: {}ms)", 
                    fullMethodName, duration, SLOW_EXECUTION_THRESHOLD_MS,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("duration", duration),
                    kv("threshold", SLOW_EXECUTION_THRESHOLD_MS),
                    kv("success", true)
            );
        } else {
            log.debug("Method execution completed: {} ({}ms)", 
                    fullMethodName, duration,
                    kv("method", fullMethodName),
                    kv("layer", layer),
                    kv("duration", duration),
                    kv("success", true)
            );
        }
    }
}
