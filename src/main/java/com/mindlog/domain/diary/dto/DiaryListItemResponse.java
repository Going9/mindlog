package com.mindlog.domain.diary.dto;

import com.mindlog.domain.tag.dto.TagResponse;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DiaryListItemResponse(
        Long id,
        LocalDate date,
        @Nullable String shortContent,
        @Nullable String situation,
        List<TagResponse> tags
) implements Serializable {
    public static DiaryListItemResponse from(DiaryMonthlySummary summary, List<TagResponse> tags) {
        return new DiaryListItemResponse(
                summary.id(),
                summary.date(),
                summary.shortContent(),
                summary.situation(),
                tags
        );
    }
}
