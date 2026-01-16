package com.mindlog.domain.diary.dto;

import org.jspecify.annotations.Nullable;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public record DiaryRequest(
                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                @Nullable String shortContent,
                @Nullable String situation,
                @Nullable String reaction,
                @Nullable String physicalSensation,
                @Nullable String desiredReaction,
                @Nullable String gratitudeMoment,
                @Nullable String selfKindWords,
                @Nullable String imageUrl) {
}
