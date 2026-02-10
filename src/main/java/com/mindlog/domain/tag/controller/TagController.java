package com.mindlog.domain.tag.controller;

import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.service.TagService;
import com.mindlog.global.security.CurrentProfileId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<List<TagResponse>> getTags(@CurrentProfileId UUID profileId) {
        var tags = tagService.getAllTags(profileId).stream()
                .map(TagResponse::from)
                .toList();
        return ResponseEntity.ok(tags);
    }

    // 새 태그 만들기 (POST /api/tags)
    @PostMapping
    public ResponseEntity<?> createTag(
            @CurrentProfileId UUID profileId,
            @RequestBody CreateTagRequest request
    ) {
        try {
            EmotionTag tag = tagService.createCustomTag(
                    profileId,
                    request.name(),
                    request.color(),
                    request.category()
            );
            return ResponseEntity.ok(TagResponse.from(tag));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 태그 이름입니다.");
        }
    }

    // 요청 데이터를 받을 DTO
    public record CreateTagRequest(String name, String color, EmotionCategory category) {}
}
