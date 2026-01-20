package com.mindlog.domain.tag.controller;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.service.TagService;
import com.mindlog.global.security.CurrentProfileId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // 태그 목록 가져오기 (GET /api/tags)
    @GetMapping
    public ResponseEntity<List<EmotionTag>> getTags(@CurrentProfileId UUID profileId) {
        return ResponseEntity.ok(tagService.getAllTags(profileId));
    }

    // 새 태그 만들기 (POST /api/tags)
    @PostMapping
    public ResponseEntity<EmotionTag> createTag(
            @CurrentProfileId UUID profileId,
            @RequestBody CreateTagRequest request
    ) {
        EmotionTag tag = tagService.createCustomTag(
                profileId,
                request.name(),
                request.color(),
                request.category()
        );
        return ResponseEntity.ok(tag);
    }

    // 요청 데이터를 받을 DTO
    public record CreateTagRequest(String name, String color, EmotionCategory category) {}
}