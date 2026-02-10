package com.mindlog.global.config;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseWarmupRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Value("${mindlog.performance.warmup-db-on-startup:true}")
    private boolean warmupEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupEnabled) {
            return;
        }

        long startedAt = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            long elapsed = System.currentTimeMillis() - startedAt;
            if (valid) {
                log.info("[DB] 커넥션 풀 워밍업 완료 ({}ms)", elapsed);
                return;
            }
            log.warn("[DB] 커넥션 검증 실패 - 워밍업이 정상 완료되지 않았습니다 ({}ms)", elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.warn("[DB] 워밍업 실패 ({}ms): {}", elapsed, e.getMessage());
        }
    }
}
