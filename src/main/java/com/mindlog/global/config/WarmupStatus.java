package com.mindlog.global.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class WarmupStatus {

    private final AtomicBoolean httpWarmupCompleted = new AtomicBoolean(false);

    public void markHttpWarmupCompleted() {
        httpWarmupCompleted.set(true);
    }

    public boolean isHttpWarmupCompleted() {
        return httpWarmupCompleted.get();
    }
}
