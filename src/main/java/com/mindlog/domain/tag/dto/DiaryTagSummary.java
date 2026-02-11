package com.mindlog.domain.tag.dto;

import com.mindlog.domain.tag.entity.EmotionCategory;
import org.jspecify.annotations.Nullable;

public record DiaryTagSummary(
        Long diaryId,
        Long tagId,
        String tagName,
        @Nullable String color,
        EmotionCategory category,
        boolean defaultTag
) {
    public TagResponse toTagResponse() {
        return new TagResponse(
                tagId,
                tagName,
                color,
                category.name(),
                defaultTag
        );
    }
}
