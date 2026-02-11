package com.mindlog.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("warmup")
public class WarmupReadinessIndicator implements HealthIndicator {

    @Value("${mindlog.performance.warmup-http-on-startup:false}")
    private boolean warmupHttpEnabled;

    private final WarmupStatus warmupStatus;

    public WarmupReadinessIndicator(WarmupStatus warmupStatus) {
        this.warmupStatus = warmupStatus;
    }

    @Override
    public Health health() {
        if (!warmupHttpEnabled) {
            return Health.up()
                    .withDetail("warmupEnabled", false)
                    .build();
        }

        if (warmupStatus.isHttpWarmupCompleted()) {
            return Health.up()
                    .withDetail("warmupEnabled", true)
                    .withDetail("httpWarmupCompleted", true)
                    .build();
        }

        return Health.outOfService()
                .withDetail("warmupEnabled", true)
                .withDetail("httpWarmupCompleted", false)
                .build();
    }
}
