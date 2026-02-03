package com.mindlog.domain.diary.dto;

import com.mindlog.domain.tag.dto.TagResponse;
import java.util.List;

public record DiaryFormDTO(
    DiaryRequest diaryRequest,
    List<TagResponse> tags,
    Long diaryId
) {}
