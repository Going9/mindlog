package com.mindlog.domain.insight.dto;

public record CategoryStat(
        String category,
        long count,
        double ratio
) {
}
