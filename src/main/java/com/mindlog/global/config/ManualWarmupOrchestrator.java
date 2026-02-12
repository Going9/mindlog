package com.mindlog.global.config;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManualWarmupOrchestrator {

    private final DatabaseWarmupRunner databaseWarmupRunner;
    private final RedisWarmupRunner redisWarmupRunner;
    private final BusinessWarmupRunner businessWarmupRunner;
    private final HttpWarmupRunner httpWarmupRunner;
    private final SupabaseWarmupRunner supabaseWarmupRunner;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean triggerAsync() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }

        Thread.ofVirtual().name("mindlog-manual-warmup").start(() -> {
            try {
                log.info("[MANUAL-WARMUP] 수동 워밍업을 시작합니다.");
                databaseWarmupRunner.warmupNow();
                redisWarmupRunner.warmupNow();
                businessWarmupRunner.warmupNow();
                httpWarmupRunner.warmupNow();
                supabaseWarmupRunner.warmupNow();
                log.info("[MANUAL-WARMUP] 수동 워밍업을 완료했습니다.");
            } catch (Exception e) {
                log.warn("[MANUAL-WARMUP] 수동 워밍업 실행 중 예외 - exception={}, message={}",
                        e.getClass().getSimpleName(),
                        e.getMessage());
                log.debug("[MANUAL-WARMUP] 수동 워밍업 예외 상세", e);
            } finally {
                running.set(false);
            }
        });

        return true;
    }
}
