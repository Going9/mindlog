package com.mindlog.domain.diary.dto;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record DiaryMonthlySummary(
        Long id,
        LocalDate date,
        @Nullable String shortContent,
        @Nullable String situation
) {
}
