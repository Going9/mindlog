package com.mindlog.domain.insight.service;

import com.mindlog.domain.insight.dto.EmotionAnalysisResponse;
import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.repository.DiaryEmotionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmotionInsightServiceTest {

    @Mock
    private DiaryEmotionRepository diaryEmotionRepository;

    @InjectMocks
    private EmotionInsightService emotionInsightService;

    @Test
    @DisplayName("감정 분석 - 카테고리/상위태그/추이를 조합해서 반환한다")
    void getEmotionAnalysis_Success() {
        UUID profileId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 10);

        given(diaryEmotionRepository.findTopTagsInRange(eq(profileId), eq(from), eq(to), any(Pageable.class)))
                .willReturn(List.of(
                        tagCount(1L, "기쁨", "#f59e0b", EmotionCategory.POSITIVE, 3L),
                        tagCount(2L, "불안", "#ef4444", EmotionCategory.NEGATIVE, 1L)
                ));
        given(diaryEmotionRepository.findDailyTrendInRange(
                profileId, from, to, EmotionCategory.POSITIVE, EmotionCategory.NEGATIVE, EmotionCategory.NEUTRAL
        )).willReturn(List.of(
                dailyTrend(LocalDate.of(2026, 2, 1), 1L, 0L, 0L, 1L, 3.0),
                dailyTrend(LocalDate.of(2026, 2, 2), 2L, 1L, 0L, 3L, 3.67)
        ));

        EmotionAnalysisResponse response = emotionInsightService.getEmotionAnalysis(profileId, from, to, 5);

        assertThat(response.totalMentions()).isEqualTo(4L);
        assertThat(response.categories()).hasSize(3);
        assertThat(response.categories().stream().filter(it -> it.category().equals("POSITIVE")).findFirst().orElseThrow().count())
                .isEqualTo(3L);
        assertThat(response.topTags()).hasSize(2);
        assertThat(response.dailyTrend()).hasSize(2);
        assertThat(response.weeklyTrend()).hasSize(2);
        assertThat(response.weeklyTrend().stream().mapToLong(it -> it.totalCount()).sum()).isEqualTo(4L);
    }

    @Test
    @DisplayName("감정 분석 - from이 to보다 늦으면 예외를 던진다")
    void getEmotionAnalysis_InvalidDateRange() {
        UUID profileId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 2, 11);
        LocalDate to = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> emotionInsightService.getEmotionAnalysis(profileId, from, to, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from 날짜");
    }

    @Test
    @DisplayName("감정 분석 - topN은 최소 1, 최대 30으로 보정된다")
    void getEmotionAnalysis_TopNClamp() {
        UUID profileId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 10);

        given(diaryEmotionRepository.findTopTagsInRange(eq(profileId), eq(from), eq(to), any(Pageable.class)))
                .willReturn(List.of());
        given(diaryEmotionRepository.findDailyTrendInRange(
                profileId, from, to, EmotionCategory.POSITIVE, EmotionCategory.NEGATIVE, EmotionCategory.NEUTRAL
        )).willReturn(List.of());

        emotionInsightService.getEmotionAnalysis(profileId, from, to, 999);

        verify(diaryEmotionRepository).findTopTagsInRange(
                eq(profileId),
                eq(from),
                eq(to),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 30)
        );
    }

    private DiaryEmotionRepository.CategoryCountView categoryCount(EmotionCategory category, Long count) {
        return new DiaryEmotionRepository.CategoryCountView() {
            @Override
            public EmotionCategory getCategory() {
                return category;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private DiaryEmotionRepository.TagCountView tagCount(
            Long tagId,
            String tagName,
            String color,
            EmotionCategory category,
            Long count
    ) {
        return new DiaryEmotionRepository.TagCountView() {
            @Override
            public Long getTagId() {
                return tagId;
            }

            @Override
            public String getTagName() {
                return tagName;
            }

            @Override
            public String getColor() {
                return color;
            }

            @Override
            public EmotionCategory getCategory() {
                return category;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private DiaryEmotionRepository.DailyTrendView dailyTrend(
            LocalDate date,
            Long positiveCount,
            Long negativeCount,
            Long neutralCount,
            Long totalCount,
            Double avgIntensity
    ) {
        return new DiaryEmotionRepository.DailyTrendView() {
            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public Long getPositiveCount() {
                return positiveCount;
            }

            @Override
            public Long getNegativeCount() {
                return negativeCount;
            }

            @Override
            public Long getNeutralCount() {
                return neutralCount;
            }

            @Override
            public Long getTotalCount() {
                return totalCount;
            }

            @Override
            public Double getAvgIntensity() {
                return avgIntensity;
            }
        };
    }
}
