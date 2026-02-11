package com.mindlog.domain.insight.dto;

import java.time.LocalDate;
import java.util.List;

public record EmotionAnalysisResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalMentions,
        List<CategoryStat> categories,
        List<TagStat> topTags,
        List<DailyTrendPoint> dailyTrend,
        List<WeeklyTrendPoint> weeklyTrend
) {
}
