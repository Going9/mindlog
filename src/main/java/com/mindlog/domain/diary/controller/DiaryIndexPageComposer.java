package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryListItemResponse;
import com.mindlog.domain.diary.service.DiaryService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

final class DiaryIndexPageComposer {

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월");
  private static final int SEARCH_PAGE_SIZE = 12;

  private final DiaryService diaryService;

  DiaryIndexPageComposer(DiaryService diaryService) {
    this.diaryService = diaryService;
  }

  DiaryIndexPage compose(
      UUID profileId,
      @Nullable Integer year,
      @Nullable Integer month,
      @Nullable String sort,
      @Nullable Long refreshToken,
      @Nullable String keyword,
      @Nullable Integer page) {
    var currentYearMonth = resolveYearMonth(year, month);
    var currentYear = currentYearMonth.getYear();
    var currentMonth = currentYearMonth.getMonthValue();
    var previous = currentYearMonth.minusMonths(1);
    var next = currentYearMonth.plusMonths(1);
    var normalizedSort = "oldest".equalsIgnoreCase(sort) ? "oldest" : "latest";
    var newestFirst = "latest".equals(normalizedSort);
    var normalizedKeyword = normalizeKeyword(keyword);
    var normalizedPage = (page != null && page >= 0) ? page : 0;

    Map<String, Object> attributes = new LinkedHashMap<>();
    putCommonAttributes(attributes, profileId, currentYearMonth, currentYear, currentMonth, previous, next, normalizedSort);

    if (normalizedKeyword != null) {
      var searchResult = diaryService.searchDiaries(
          profileId,
          normalizedKeyword,
          null,
          null,
          newestFirst,
          normalizedPage,
          SEARCH_PAGE_SIZE);

      attributes.put("diaries", searchResult.getContent());
      attributes.put("keyword", normalizedKeyword);
      attributes.put("page", normalizedPage);
      attributes.put("hasPrev", searchResult.hasPrevious());
      attributes.put("hasNext", searchResult.hasNext());
      attributes.put("prevPage", searchResult.hasPrevious() ? normalizedPage - 1 : 0);
      attributes.put("nextPage", searchResult.hasNext() ? normalizedPage + 1 : normalizedPage);
      attributes.put("totalPages", searchResult.getTotalPages());
      attributes.put("totalElements", searchResult.getTotalElements());
      attributes.put("pageSize", SEARCH_PAGE_SIZE);
      attributes.put("isSearchMode", true);
      return new DiaryIndexPage(attributes);
    }

    List<DiaryListItemResponse> diaries = refreshToken != null
        ? diaryService.getMonthlyDiariesFresh(profileId, currentYear, currentMonth, newestFirst)
        : diaryService.getMonthlyDiaries(profileId, currentYear, currentMonth, newestFirst);

    attributes.put("diaries", diaries);
    attributes.put("keyword", null);
    attributes.put("page", 0);
    attributes.put("hasPrev", false);
    attributes.put("hasNext", false);
    attributes.put("totalPages", 0);
    attributes.put("totalElements", 0L);
    attributes.put("pageSize", SEARCH_PAGE_SIZE);
    attributes.put("isSearchMode", false);

    return new DiaryIndexPage(attributes);
  }

  private void putCommonAttributes(
      Map<String, Object> attributes,
      UUID profileId,
      YearMonth currentYearMonth,
      int year,
      int month,
      YearMonth previous,
      YearMonth next,
      String normalizedSort) {
    attributes.put("year", year);
    attributes.put("month", month);
    attributes.put("monthLabel", currentYearMonth.format(MONTH_FORMATTER));
    attributes.put("prevYear", previous.getYear());
    attributes.put("prevMonth", previous.getMonthValue());
    attributes.put("nextYear", next.getYear());
    attributes.put("nextMonth", next.getMonthValue());
    attributes.put("yearOptions", diaryService.getAvailableYears(profileId, year));
    attributes.put("monthOptions", IntStream.rangeClosed(1, 12).boxed().toList());
    attributes.put("sort", normalizedSort);
  }

  private YearMonth resolveYearMonth(@Nullable Integer year, @Nullable Integer month) {
    var now = LocalDate.now();
    var resolvedYear = (year != null) ? year : now.getYear();
    var resolvedMonth = (month != null) ? month : now.getMonthValue();

    try {
      return YearMonth.of(resolvedYear, resolvedMonth);
    } catch (RuntimeException ignored) {
      return YearMonth.of(now.getYear(), now.getMonthValue());
    }
  }

  @Nullable
  private String normalizeKeyword(@Nullable String keyword) {
    if (keyword == null) {
      return null;
    }

    var trimmed = keyword.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  record DiaryIndexPage(Map<String, Object> attributes) {
  }
}
