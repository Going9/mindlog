package com.mindlog.domain.diary.dto;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record DiaryRequest(
    LocalDate date,
    @Nullable String shortContent,
    @Nullable String situation,
    @Nullable String reaction,
    @Nullable String physicalSensation,
    @Nullable String desiredReaction,
    @Nullable String gratitudeMoment,
    @Nullable String selfKindWords,
    @Nullable String imageUrl
) {}
