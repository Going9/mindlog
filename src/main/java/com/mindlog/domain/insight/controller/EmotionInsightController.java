package com.mindlog.domain.insight.controller;

import com.mindlog.domain.insight.service.EmotionInsightService;
import com.mindlog.global.security.CurrentProfileId;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class EmotionInsightController {

    private final EmotionInsightService emotionInsightService;

    @GetMapping("/emotions")
    public ResponseEntity<?> getEmotionAnalysis(
            @CurrentProfileId UUID profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable Integer topN
    ) {
        try {
            return ResponseEntity.ok(emotionInsightService.getEmotionAnalysis(profileId, from, to, topN));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
        }
    }
}
