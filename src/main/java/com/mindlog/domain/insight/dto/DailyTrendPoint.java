package com.mindlog.domain.insight.dto;

import java.time.LocalDate;

public record DailyTrendPoint(
        LocalDate date,
        long positiveCount,
        long negativeCount,
        long neutralCount,
        long totalCount,
        double avgIntensity
) {
}
