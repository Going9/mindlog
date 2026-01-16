package com.mindlog.domain.diary.dto;

import com.mindlog.domain.diary.entity.Diary;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

public record DiaryResponse(
        Long id,
        LocalDate date,
        @Nullable String shortContent,
        @Nullable String situation,
        @Nullable String reaction,
        @Nullable String physicalSensation,
        @Nullable String desiredReaction,
        @Nullable String gratitudeMoment,
        @Nullable String selfKindWords,
        @Nullable String imageUrl,
        Instant createdAt,
        Instant updatedAt) {
    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getId(),
                diary.getDate(),
                diary.getShortContent(),
                diary.getSituation(),
                diary.getReaction(),
                diary.getPhysicalSensation(),
                diary.getDesiredReaction(),
                diary.getGratitudeMoment(),
                diary.getSelfKindWords(),
                diary.getImageUrl(),
                diary.getCreatedAt(),
                diary.getUpdatedAt());
    }
}
