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
    private static final String EMPTY_TAG_NAME_MESSAGE = "태그 이름을 입력해주세요.";
    private static final String EMPTY_CATEGORY_MESSAGE = "카테고리를 선택해주세요.";
    private static final String TAG_NOT_FOUND_MESSAGE = "삭제할 태그를 찾을 수 없습니다.";
    private static final String DEFAULT_TAG_DELETE_MESSAGE = "기본 태그는 삭제할 수 없습니다.";

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
        var normalizedName = normalizeTagName(name);
        validateCategory(category);

        // 1. 동일 이름이 이미 있으면 기존 태그를 반환 (중복 클릭/동시 요청 보호)
        var existingTag = findExistingTag(profileId, normalizedName);
        if (existingTag != null) {
            return existingTag;
        }

        // 2. 태그 생성 및 저장
        var newTag = buildCustomTag(profileId, normalizedName, color, category);

        try {
            return emotionTagRepository.save(newTag);
        } catch (DataIntegrityViolationException e) {
            return emotionTagRepository.findByProfileIdAndName(profileId, normalizedName)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * 커스텀 태그 삭제
     */
    @Transactional
    public void deleteCustomTag(UUID profileId, Long tagId) {
        var tag = emotionTagRepository.findByIdAndProfileId(tagId, profileId)
                .orElseThrow(() -> new IllegalArgumentException(TAG_NOT_FOUND_MESSAGE));

        if (tag.isDefault()) {
            throw new IllegalArgumentException(DEFAULT_TAG_DELETE_MESSAGE);
        }

        emotionTagRepository.delete(tag);
    }

    private String normalizeTagName(String name) {
        var normalizedName = (name == null) ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(EMPTY_TAG_NAME_MESSAGE);
        }
        return normalizedName;
    }

    private void validateCategory(EmotionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException(EMPTY_CATEGORY_MESSAGE);
        }
    }

    private EmotionTag buildCustomTag(UUID profileId, String name, String color, EmotionCategory category) {
        return EmotionTag.builder()
                .profileId(profileId)
                .name(name)
                .color(color)
                .category(category)
                .isDefault(false)
                .build();
    }

    private EmotionTag findExistingTag(UUID profileId, String normalizedName) {
        return emotionTagRepository.findByProfileIdAndName(profileId, normalizedName).orElse(null);
    }
}
