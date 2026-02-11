package com.mindlog.domain.insight.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record WeeklyTrendPoint(
        LocalDate weekStart,
        LocalDate weekEnd,
        long positiveCount,
        long negativeCount,
        long neutralCount,
        long totalCount,
        double avgIntensity,
        double positiveRatio,
        double negativeRatio,
        double neutralRatio
) implements Serializable {
}
