package com.mindlog.domain.tag.service;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final EmotionTagRepository emotionTagRepository;

    /**
     * 일기 작성 시 보여줄 태그 목록 조회
     * - 공용 태그(isDefault=true) + 내 커스텀 태그(profileId=내꺼)
     */
    public List<EmotionTag> getAllTags(UUID profileId) {
        return emotionTagRepository.findAvailableTagsForProfile(profileId);
    }

    /**
     * 커스텀 태그 생성
     */
    @Transactional
    public EmotionTag createCustomTag(UUID profileId, String name, String color, EmotionCategory category) {
        var normalizedName = (name == null) ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("태그 이름을 입력해주세요.");
        }

        if (category == null) {
            throw new IllegalArgumentException("카테고리를 선택해주세요.");
        }

        // 1. 동일 이름이 이미 있으면 기존 태그를 반환 (중복 클릭/동시 요청 보호)
        var existingTag = emotionTagRepository.findByProfileIdAndName(profileId, normalizedName);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        // 2. 태그 생성 및 저장
        EmotionTag newTag = EmotionTag.builder()
                .profileId(profileId)
                .name(normalizedName)
                .color(color)
                .category(category)
                .isDefault(false) // 커스텀 태그이므로 false
                .build();

        try {
            return emotionTagRepository.save(newTag);
        } catch (DataIntegrityViolationException e) {
            return emotionTagRepository.findByProfileIdAndName(profileId, normalizedName)
                    .orElseThrow(() -> e);
        }
    }
}
