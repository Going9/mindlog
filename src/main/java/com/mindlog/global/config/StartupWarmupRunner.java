package com.mindlog.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupWarmupRunner {

    @Value("${mindlog.performance.startup-warmup-on-startup:false}")
    private boolean startupWarmupEnabled;

    @Value("${mindlog.performance.startup-warmup-delay-seconds:10}")
    private int startupWarmupDelaySeconds;

    private final ManualWarmupOrchestrator manualWarmupOrchestrator;
    private final WarmupStatus warmupStatus;

    public StartupWarmupRunner(ManualWarmupOrchestrator manualWarmupOrchestrator, WarmupStatus warmupStatus) {
        this.manualWarmupOrchestrator = manualWarmupOrchestrator;
        this.warmupStatus = warmupStatus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!startupWarmupEnabled) {
            warmupStatus.markStartupWarmupCompleted();
            log.info("[STARTUP-WARMUP] 비활성화되어 워밍업 게이트를 즉시 해제합니다.");
            return;
        }

        var delaySeconds = Math.max(0, startupWarmupDelaySeconds);
        Thread.ofVirtual().name("mindlog-startup-warmup").start(() -> {
            try {
                if (delaySeconds > 0) {
                    log.info("[STARTUP-WARMUP] {}초 대기 후 워밍업을 시작합니다.", delaySeconds);
                    Thread.sleep(delaySeconds * 1000L);
                }
                manualWarmupOrchestrator.runStartupWarmup();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                warmupStatus.markStartupWarmupCompleted();
                log.warn("[STARTUP-WARMUP] 지연 대기 중 인터럽트되어 워밍업 게이트를 해제합니다.");
            }
        });
    }
}
