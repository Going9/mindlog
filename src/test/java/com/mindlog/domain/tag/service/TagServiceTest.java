package com.mindlog.domain.tag.service;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private EmotionTagRepository emotionTagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    @DisplayName("태그 목록 조회 - 공용 태그와 내 태그를 모두 가져온다")
    void getAllTags() {
        // given
        UUID profileId = UUID.randomUUID();
        EmotionTag defaultTag = EmotionTag.builder()
                .name("행복")
                .isDefault(true)
                .category(EmotionCategory.POSITIVE)
                .build();
        EmotionTag myTag = EmotionTag.builder()
                .profileId(profileId)
                .name("나만의태그")
                .isDefault(false)
                .category(EmotionCategory.NEUTRAL)
                .build();

        given(emotionTagRepository.findAvailableTagsForProfile(profileId))
                .willReturn(List.of(defaultTag, myTag));

        // when
        List<EmotionTag> result = tagService.getAllTags(profileId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(defaultTag, myTag);
    }

    @Test
    @DisplayName("커스텀 태그 생성 - 성공")
    void createCustomTag_Success() {
        // given
        UUID profileId = UUID.randomUUID();
        String name = "새태그";
        String color = "#FFFFFF";
        EmotionCategory category = EmotionCategory.POSITIVE;

        given(emotionTagRepository.existsByProfileIdAndName(profileId, name)).willReturn(false);
        given(emotionTagRepository.save(any(EmotionTag.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        EmotionTag result = tagService.createCustomTag(profileId, name, color, category);

        // then
        assertThat(result.getProfileId()).isEqualTo(profileId);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.isDefault()).isFalse();
        verify(emotionTagRepository).save(any(EmotionTag.class));
    }

    @Test
    @DisplayName("커스텀 태그 생성 - 중복 이름이면 실패")
    void createCustomTag_DuplicateName() {
        // given
        UUID profileId = UUID.randomUUID();
        String name = "중복태그";

        given(emotionTagRepository.existsByProfileIdAndName(profileId, name)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> tagService.createCustomTag(profileId, name, "#000", EmotionCategory.NEGATIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 태그 이름입니다.");
    }
}
