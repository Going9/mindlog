package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.exception.DuplicateDiaryDateException;
import com.mindlog.domain.diary.repository.DiaryRepository;
import com.mindlog.domain.tag.entity.DiaryTag;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.DiaryTagRepository;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private DiaryTagRepository diaryTagRepository;
    @Mock
    private EmotionTagRepository emotionTagRepository;

    @InjectMocks
    private DiaryService diaryService;

    @Test
    @DisplayName("일기 생성 - 성공")
    void createDiary_Success() {
        // given
        UUID profileId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        DiaryRequest request = new DiaryRequest(
                today, "content", "situation", "reaction", "sensation",
                "desired", "gratitude", "selfKind", "imgUrl", List.of(1L, 2L)
        );

        given(diaryRepository.existsByProfileIdAndDate(profileId, today)).willReturn(false);
        given(diaryRepository.save(any(Diary.class))).willAnswer(inv -> {
            Diary d = inv.getArgument(0);
            return d; 
        });

        List<EmotionTag> tags = List.of(
            EmotionTag.builder().name("tag1").build(),
            EmotionTag.builder().name("tag2").build()
        );
        given(emotionTagRepository.findAllById(anyList())).willReturn(tags);

        // when
        diaryService.createDiary(profileId, request);

        // then
        verify(diaryRepository).save(any(Diary.class));
        verify(diaryTagRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("일기 생성 - 실패 (중복 날짜)")
    void createDiary_Fail_DuplicateDate() {
        // given
        UUID profileId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        DiaryRequest request = new DiaryRequest(
                today, "content", null, null, null, null, null, null, null, null
        );

        given(diaryRepository.existsByProfileIdAndDate(profileId, today)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> diaryService.createDiary(profileId, request))
                .isInstanceOf(DuplicateDiaryDateException.class)
                .hasMessageContaining("이미 해당 날짜에 일기가 존재합니다");
    }

    @Test
    @DisplayName("일기 상세 조회 - 성공")
    void getDiary_Success() {
        // given
        UUID profileId = UUID.randomUUID();
        Long diaryId = 1L;
        Diary diary = Diary.builder()
                .profileId(profileId)
                .date(LocalDate.now())
                .shortContent("content")
                .build();

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
        given(diaryTagRepository.findByDiaryId(diaryId)).willReturn(List.of());

        // when
        DiaryResponse response = diaryService.getDiary(profileId, diaryId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.profileId()).isEqualTo(profileId);
    }
}
