package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryFormDTO;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.service.TagService;
import com.mindlog.domain.tag.entity.EmotionTag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryFormService {

    private final DiaryService diaryService;
    private final TagService tagService;

    public DiaryFormDTO getCreateForm(UUID profileId) {
        var tags = tagService.getAllTags(profileId).stream()
                .map(TagResponse::from)
                .toList();
        
        var emptyRequest = new DiaryRequest(
            LocalDate.now(), null, null, null, null, null, null, null, null, null
        );

        return new DiaryFormDTO(emptyRequest, tags, null);
    }

    public DiaryFormDTO getEditForm(UUID profileId, Long diaryId) {
        var tags = tagService.getAllTags(profileId).stream()
                .map(TagResponse::from)
                .toList();

        var diary = diaryService.getDiary(profileId, diaryId);
        var existingTagIds = diary.tags().stream().map(TagResponse::id).toList();

        var request = new DiaryRequest(
            diary.date(), diary.shortContent(), diary.situation(), diary.reaction(),
            diary.physicalSensation(), diary.desiredReaction(), diary.gratitudeMoment(),
            diary.selfKindWords(), diary.imageUrl(), existingTagIds
        );

        return new DiaryFormDTO(request, tags, diaryId);
    }

    public DiaryFormDTO getFormOnError(UUID profileId, DiaryRequest request, Long diaryId) {
        var tags = tagService.getAllTags(profileId).stream()
                .map(TagResponse::from)
                .toList();
        
        return new DiaryFormDTO(request, tags, diaryId);
    }
}
