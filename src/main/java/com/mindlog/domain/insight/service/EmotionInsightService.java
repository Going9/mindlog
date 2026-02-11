package com.mindlog.domain.insight.service;

import com.mindlog.domain.insight.dto.CategoryStat;
import com.mindlog.domain.insight.dto.DailyTrendPoint;
import com.mindlog.domain.insight.dto.EmotionAnalysisResponse;
import com.mindlog.domain.insight.dto.TagStat;
import com.mindlog.domain.insight.dto.WeeklyTrendPoint;
import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.repository.DiaryEmotionRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionInsightService {

    private static final int DEFAULT_TOP_TAG_COUNT = 10;
    private static final int MIN_TOP_TAG_COUNT = 1;
    private static final int MAX_TOP_TAG_COUNT = 30;
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final DiaryEmotionRepository diaryEmotionRepository;

    public EmotionAnalysisResponse getEmotionAnalysis(
            UUID profileId,
            @Nullable LocalDate fromDate,
            @Nullable LocalDate toDate,
            @Nullable Integer topN
    ) {
        var dateRange = resolveDateRange(fromDate, toDate);
        var resolvedFromDate = dateRange.fromDate();
        var resolvedToDate = dateRange.toDate();

        var normalizedTopN = normalizeTopN(topN);

        var dailyTrend = loadDailyTrend(profileId, resolvedFromDate, resolvedToDate);
        var totalMentions = calculateTotalMentions(dailyTrend);
        var categoryStats = toCategoryStats(dailyTrend, totalMentions);
        var topTags = loadTopTags(profileId, resolvedFromDate, resolvedToDate, normalizedTopN);
        var weeklyTrend = buildWeeklyTrend(dailyTrend);

        return new EmotionAnalysisResponse(
                resolvedFromDate,
                resolvedToDate,
                totalMentions,
                categoryStats,
                topTags,
                dailyTrend,
                weeklyTrend
        );
    }

    private DateRange resolveDateRange(@Nullable LocalDate fromDate, @Nullable LocalDate toDate) {
        var resolvedToDate = (toDate != null) ? toDate : LocalDate.now();
        var resolvedFromDate = (fromDate != null) ? fromDate : resolvedToDate.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (resolvedFromDate.isAfter(resolvedToDate)) {
            throw new IllegalArgumentException("from 날짜는 to 날짜보다 늦을 수 없습니다.");
        }
        return new DateRange(resolvedFromDate, resolvedToDate);
    }

    private long calculateTotalMentions(List<DailyTrendPoint> dailyTrend) {
        return dailyTrend.stream().mapToLong(DailyTrendPoint::totalCount).sum();
    }

    private List<CategoryStat> toCategoryStats(List<DailyTrendPoint> dailyTrend, long totalMentions) {
        long positiveCount = dailyTrend.stream().mapToLong(DailyTrendPoint::positiveCount).sum();
        long negativeCount = dailyTrend.stream().mapToLong(DailyTrendPoint::negativeCount).sum();
        long neutralCount = dailyTrend.stream().mapToLong(DailyTrendPoint::neutralCount).sum();

        return Arrays.stream(EmotionCategory.values())
                .map(category -> {
                    var count = switch (category) {
                        case POSITIVE -> positiveCount;
                        case NEGATIVE -> negativeCount;
                        case NEUTRAL -> neutralCount;
                    };
                    var ratio = (totalMentions == 0L) ? 0.0 : (double) count / (double) totalMentions;
                    return new CategoryStat(category.name(), count, ratio);
                })
                .toList();
    }

    private List<TagStat> loadTopTags(UUID profileId, LocalDate fromDate, LocalDate toDate, int normalizedTopN) {
        return diaryEmotionRepository.findTopTagsInRange(
                        profileId,
                        fromDate,
                        toDate,
                        PageRequest.of(0, normalizedTopN))
                .stream()
                .map(it -> new TagStat(
                        it.getTagId(),
                        it.getTagName(),
                        it.getColor(),
                        it.getCategory().name(),
                        nonNullLong(it.getCount())))
                .toList();
    }

    private List<DailyTrendPoint> loadDailyTrend(UUID profileId, LocalDate fromDate, LocalDate toDate) {
        return diaryEmotionRepository.findDailyTrendInRange(
                        profileId,
                        fromDate,
                        toDate,
                        EmotionCategory.POSITIVE,
                        EmotionCategory.NEGATIVE,
                        EmotionCategory.NEUTRAL)
                .stream()
                .map(it -> new DailyTrendPoint(
                        it.getDate(),
                        nonNullLong(it.getPositiveCount()),
                        nonNullLong(it.getNegativeCount()),
                        nonNullLong(it.getNeutralCount()),
                        nonNullLong(it.getTotalCount()),
                        (it.getAvgIntensity() == null) ? 0.0 : it.getAvgIntensity()))
                .toList();
    }

    private List<WeeklyTrendPoint> buildWeeklyTrend(List<DailyTrendPoint> dailyTrend) {
        var buckets = new LinkedHashMap<LocalDate, WeeklyAccumulator>();

        for (var day : dailyTrend) {
            var weekStart = day.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            var bucket = buckets.computeIfAbsent(weekStart, ignored -> new WeeklyAccumulator());
            bucket.positiveCount += day.positiveCount();
            bucket.negativeCount += day.negativeCount();
            bucket.neutralCount += day.neutralCount();
            bucket.totalCount += day.totalCount();
            bucket.intensityWeightedSum += day.avgIntensity() * day.totalCount();
        }

        return buckets.entrySet().stream()
                .map(entry -> {
                    var weekStart = entry.getKey();
                    var weekEnd = weekStart.plusDays(6);
                    var acc = entry.getValue();
                    var total = acc.totalCount;
                    var avgIntensity = (total == 0L) ? 0.0 : acc.intensityWeightedSum / total;
                    var positiveRatio = (total == 0L) ? 0.0 : (double) acc.positiveCount / total;
                    var negativeRatio = (total == 0L) ? 0.0 : (double) acc.negativeCount / total;
                    var neutralRatio = (total == 0L) ? 0.0 : (double) acc.neutralCount / total;
                    return new WeeklyTrendPoint(
                            weekStart,
                            weekEnd,
                            acc.positiveCount,
                            acc.negativeCount,
                            acc.neutralCount,
                            total,
                            avgIntensity,
                            positiveRatio,
                            negativeRatio,
                            neutralRatio
                    );
                })
                .toList();
    }

    private int normalizeTopN(@Nullable Integer topN) {
        if (topN == null) {
            return DEFAULT_TOP_TAG_COUNT;
        }
        return Math.clamp(topN, MIN_TOP_TAG_COUNT, MAX_TOP_TAG_COUNT);
    }

    private long nonNullLong(@Nullable Long value) {
        return (value == null) ? 0L : value;
    }

    private static final class WeeklyAccumulator {
        private long positiveCount;
        private long negativeCount;
        private long neutralCount;
        private long totalCount;
        private double intensityWeightedSum;
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }
}
