package com.mindlog.global.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class WarmupStatus {

    private final AtomicBoolean httpWarmupCompleted = new AtomicBoolean(false);
    private final AtomicBoolean supabaseWarmupCompleted = new AtomicBoolean(false);

    public void markHttpWarmupCompleted() {
        httpWarmupCompleted.set(true);
    }

    public boolean isHttpWarmupCompleted() {
        return httpWarmupCompleted.get();
    }

    public void markSupabaseWarmupCompleted() {
        supabaseWarmupCompleted.set(true);
    }

    public boolean isSupabaseWarmupCompleted() {
        return supabaseWarmupCompleted.get();
    }
}
