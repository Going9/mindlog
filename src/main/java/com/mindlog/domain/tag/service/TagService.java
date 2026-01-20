package com.mindlog.domain.tag.service;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import lombok.RequiredArgsConstructor;
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
        // 1. 중복 이름 체크 (내 태그 중에서만)
        if (emotionTagRepository.existsByProfileIdAndName(profileId, name)) {
            throw new IllegalArgumentException("이미 존재하는 태그 이름입니다.");
        }

        // 2. 태그 생성 및 저장
        EmotionTag newTag = EmotionTag.builder()
                .profileId(profileId)
                .name(name)
                .color(color)
                .category(category)
                .isDefault(false) // 커스텀 태그이므로 false
                .build();

        return emotionTagRepository.save(newTag);
    }
}