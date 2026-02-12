package com.mindlog.global.config;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManualWarmupController {

    private static final String TOKEN_HEADER = "X-Warmup-Token";

    @Value("${mindlog.performance.manual-warmup-token:}")
    private String manualWarmupToken;

    private final ManualWarmupOrchestrator manualWarmupOrchestrator;

    public ManualWarmupController(ManualWarmupOrchestrator manualWarmupOrchestrator) {
        this.manualWarmupOrchestrator = manualWarmupOrchestrator;
    }

    @PostMapping("/internal/warmup/run")
    public ResponseEntity<Map<String, String>> triggerWarmup(
            @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        if (!StringUtils.hasText(manualWarmupToken)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "manual warmup token is not configured"));
        }
        if (!manualWarmupToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "forbidden"));
        }

        var triggered = manualWarmupOrchestrator.triggerAsync();
        if (!triggered) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "manual warmup is already running"));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "manual warmup started"));
    }
}
