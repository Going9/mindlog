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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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

        given(emotionTagRepository.findByProfileIdAndName(profileId, name)).willReturn(Optional.empty());
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
    @DisplayName("커스텀 태그 생성 - 중복 이름이면 기존 태그 반환")
    void createCustomTag_DuplicateName() {
        // given
        UUID profileId = UUID.randomUUID();
        String name = "중복태그";
        EmotionTag existing = EmotionTag.builder()
                .profileId(profileId)
                .name(name)
                .isDefault(false)
                .category(EmotionCategory.NEGATIVE)
                .build();

        given(emotionTagRepository.findByProfileIdAndName(profileId, name)).willReturn(Optional.of(existing));

        // when
        EmotionTag result = tagService.createCustomTag(profileId, name, "#000", EmotionCategory.NEGATIVE);

        // then
        assertThat(result).isEqualTo(existing);
        verify(emotionTagRepository, never()).save(any(EmotionTag.class));
    }

    @Test
    @DisplayName("커스텀 태그 삭제 - 성공")
    void deleteCustomTag_Success() {
        // given
        UUID profileId = UUID.randomUUID();
        Long tagId = 1L;
        EmotionTag customTag = EmotionTag.builder()
                .profileId(profileId)
                .name("삭제대상")
                .isDefault(false)
                .category(EmotionCategory.NEUTRAL)
                .build();

        given(emotionTagRepository.findByIdAndProfileId(tagId, profileId)).willReturn(Optional.of(customTag));

        // when
        tagService.deleteCustomTag(profileId, tagId);

        // then
        verify(emotionTagRepository).delete(customTag);
    }

    @Test
    @DisplayName("커스텀 태그 삭제 - 기본 태그는 삭제할 수 없다")
    void deleteCustomTag_DefaultTag() {
        // given
        UUID profileId = UUID.randomUUID();
        Long tagId = 1L;
        EmotionTag defaultTag = EmotionTag.builder()
                .profileId(profileId)
                .name("기본")
                .isDefault(true)
                .category(EmotionCategory.POSITIVE)
                .build();

        given(emotionTagRepository.findByIdAndProfileId(tagId, profileId)).willReturn(Optional.of(defaultTag));

        // when & then
        assertThatThrownBy(() -> tagService.deleteCustomTag(profileId, tagId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기본 태그는 삭제할 수 없습니다.");

        verify(emotionTagRepository, never()).delete(any(EmotionTag.class));
    }

    @Test
    @DisplayName("커스텀 태그 삭제 - 내 태그가 아니면 실패")
    void deleteCustomTag_NotFound() {
        // given
        UUID profileId = UUID.randomUUID();
        Long tagId = 99L;
        given(emotionTagRepository.findByIdAndProfileId(tagId, profileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tagService.deleteCustomTag(profileId, tagId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 태그를 찾을 수 없습니다.");
    }
}
