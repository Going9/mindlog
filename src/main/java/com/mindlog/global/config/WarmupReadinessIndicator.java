package com.mindlog.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("warmup")
public class WarmupReadinessIndicator implements HealthIndicator {

    @Value("${mindlog.performance.warmup-http-on-startup:false}")
    private boolean warmupHttpEnabled;

    @Value("${mindlog.performance.warmup-supabase-on-startup:false}")
    private boolean warmupSupabaseEnabled;

    private final WarmupStatus warmupStatus;

    public WarmupReadinessIndicator(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    public Health health() {
        boolean httpDone = !warmupHttpEnabled || warmupStatus.isHttpWarmupCompleted();
        boolean supabaseDone = !warmupSupabaseEnabled || warmupStatus.isSupabaseWarmupCompleted();
        boolean allDone = httpDone && supabaseDone;

        if (allDone) {
            return Health.up()
                    .withDetail("httpWarmupEnabled", warmupHttpEnabled)
                    .withDetail("httpWarmupCompleted", httpDone)
                    .withDetail("supabaseWarmupEnabled", warmupSupabaseEnabled)
                    .withDetail("supabaseWarmupCompleted", supabaseDone)
                    .build();
        }

        return Health.outOfService()
                .withDetail("httpWarmupEnabled", warmupHttpEnabled)
                .withDetail("httpWarmupCompleted", httpDone)
                .withDetail("supabaseWarmupEnabled", warmupSupabaseEnabled)
                .withDetail("supabaseWarmupCompleted", supabaseDone)
                .build();
    }
}
