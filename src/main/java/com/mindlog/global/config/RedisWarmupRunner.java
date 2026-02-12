package com.mindlog.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class RedisWarmupRunner implements ApplicationRunner {

    private static final String WARMUP_KEY = "mindlog:warmup:redis";
    private static final String WARMUP_VALUE = "ok";

    private final StringRedisTemplate redisTemplate;

    @Value("${mindlog.performance.warmup-redis-on-startup:true}")
    private boolean warmupEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            return;
        }

        var startedAt = System.currentTimeMillis();
        try {
            var pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());

            redisTemplate.opsForValue().set(WARMUP_KEY, WARMUP_VALUE);
            var value = redisTemplate.opsForValue().get(WARMUP_KEY);
            redisTemplate.delete(WARMUP_KEY);

            var elapsed = System.currentTimeMillis() - startedAt;
            log.info("[REDIS] 워밍업 완료 - ping={}, setGetDelete={}, elapsed={}ms",
                    pong,
                    WARMUP_VALUE.equals(value),
                    elapsed);
        } catch (Exception e) {
            var elapsed = System.currentTimeMillis() - startedAt;
            log.warn("[REDIS] 워밍업 실패 - elapsed={}ms, message={}", elapsed, e.getMessage());
        }
    }
}
