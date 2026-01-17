package com.mindlog.domain.diary.dto;

import com.mindlog.domain.diary.entity.Diary;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record DiaryResponse(
    Long id,
    UUID profileId,
    LocalDate date,
    @Nullable String shortContent,
    @Nullable String situation,
    @Nullable String reaction,
    @Nullable String physicalSensation,
    @Nullable String desiredReaction,
    @Nullable String gratitudeMoment,
    @Nullable String selfKindWords,
    @Nullable String imageUrl
) {
    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
            diary.getId(),
            diary.getProfileId(),
            diary.getDate(),
            diary.getShortContent(),
            diary.getSituation(),
            diary.getReaction(),
            diary.getPhysicalSensation(),
            diary.getDesiredReaction(),
            diary.getGratitudeMoment(),
            diary.getSelfKindWords(),
            diary.getImageUrl()
        );
    }
}
