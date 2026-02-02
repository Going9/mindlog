package com.mindlog.domain.diary.dto;

import jakarta.validation.constraints.NotNull; // 추가
import jakarta.validation.constraints.Size;    // 추가
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DiaryRequest(
        @NotNull(message = "날짜를 선택해주세요.") // 필수
        LocalDate date,

        @Nullable
        @Size(max = 100, message = "한줄 일기는 100자 이내로 작성해주세요.") // 선택이지만 길이는 제한
        String shortContent,

        // 나머지는 선택값이므로 그대로 둠 (@Nullable만 유지)
        @Nullable String situation,
        @Nullable String reaction,
        @Nullable String physicalSensation,
        @Nullable String desiredReaction,
        @Nullable String gratitudeMoment,
        @Nullable String selfKindWords,
        @Nullable String imageUrl,
        @Nullable List<Long> tagIds
) {}