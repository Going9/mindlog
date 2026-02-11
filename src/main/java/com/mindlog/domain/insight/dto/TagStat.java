package com.mindlog.domain.insight.dto;

import org.jspecify.annotations.Nullable;

public record TagStat(
        Long tagId,
        String name,
        @Nullable String color,
        String category,
        long count
) {
}
