package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryListItemResponse;
import com.mindlog.domain.diary.dto.DiaryMonthlySummary;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.exception.DuplicateDiaryDateException;
import com.mindlog.domain.diary.repository.DiaryRepository;
import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.repository.DiaryEmotionRepository;
import com.mindlog.domain.tag.repository.DiaryTagRepository;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DiaryService {

  private static final String DIARY_NOT_FOUND_MESSAGE = "Diary not found";
  private static final String UNAUTHORIZED_ACCESS_MESSAGE = "Unauthorized access";
  private static final long SEARCH_DEFAULT_RANGE_DAYS = 365;

  private final DiaryRepository diaryRepository;
  private final DiaryTagSupport diaryTagSupport;
  private final DiaryYearOptionsSupport diaryYearOptionsSupport;

  public DiaryService(
      DiaryRepository diaryRepository,
      DiaryEmotionRepository diaryEmotionRepository,
      DiaryTagRepository diaryTagRepository,
      EmotionTagRepository emotionTagRepository) {
    this.diaryRepository = diaryRepository;
    this.diaryTagSupport = new DiaryTagSupport(
        diaryEmotionRepository,
        diaryTagRepository,
        emotionTagRepository);
    this.diaryYearOptionsSupport = new DiaryYearOptionsSupport(diaryRepository);
  }

  @Cacheable(
      cacheNames = "monthlyDiaries",
      key = "#profileId.toString() + '|' + #year + '|' + #month + '|' + #newestFirst"
  )
  public List<DiaryListItemResponse> getMonthlyDiaries(
      UUID profileId,
      int year,
      int month,
      boolean newestFirst) {
    return loadMonthlyDiaries(profileId, year, month, newestFirst);
  }

  /**
   * 캐시 우회가 필요한 호출(예: 삭제 직후 강제 갱신)에 사용한다.
   */
  public List<DiaryListItemResponse> getMonthlyDiariesFresh(
      UUID profileId,
      int year,
      int month,
      boolean newestFirst) {
    return loadMonthlyDiaries(profileId, year, month, newestFirst);
  }

  private List<DiaryListItemResponse> loadMonthlyDiaries(
      UUID profileId,
      int year,
      int month,
      boolean newestFirst) {
    YearMonth yearMonth = resolveYearMonth(year, month);
    List<DiaryMonthlySummary> diaries = findDiariesByMonth(profileId, yearMonth, newestFirst);
    if (diaries.isEmpty()) {
      return List.of();
    }

    Map<Long, List<TagResponse>> tagsByDiaryId = diaryTagSupport.fetchAndGroupTags(extractDiaryIds(diaries));
    return buildDiaryListResponses(diaries, tagsByDiaryId);
  }

  public List<Integer> getAvailableYears(UUID profileId, int selectedYear) {
    return diaryYearOptionsSupport.getAvailableYears(profileId, selectedYear);
  }

  private List<DiaryMonthlySummary> findDiariesByMonth(
      UUID profileId,
      YearMonth yearMonth,
      boolean newestFirst) {
    LocalDate start = yearMonth.atDay(1);
    LocalDate end = yearMonth.atEndOfMonth();
    if (newestFirst) {
      return diaryRepository.findMonthlySummaryByProfileIdAndDateBetweenDesc(profileId, start, end)
          .stream()
          .map(DiaryRepository.DiaryMonthlySummaryRow::toSummary)
          .toList();
    }
    return diaryRepository.findMonthlySummaryByProfileIdAndDateBetween(profileId, start, end)
        .stream()
        .map(DiaryRepository.DiaryMonthlySummaryRow::toSummary)
        .toList();
  }

  public Page<DiaryListItemResponse> searchDiaries(
      UUID profileId,
      @Nullable String keyword,
      @Nullable LocalDate fromDate,
      @Nullable LocalDate toDate,
      boolean newestFirst,
      int page,
      int size) {
    var normalizedKeyword = normalizeKeyword(keyword);
    if (normalizedKeyword == null) {
      return Page.empty(PageRequest.of(page, size));
    }

    var normalizedDateRange = normalizeDateRange(fromDate, toDate);
    var pageable = PageRequest.of(page, size);
    Page<DiaryRepository.DiaryMonthlySummaryRow> summaryRows;
    if (newestFirst) {
      summaryRows = diaryRepository.searchByKeywordOrderByDateDesc(
          profileId,
          normalizedKeyword,
          normalizedDateRange.fromDate(),
          normalizedDateRange.toDate(),
          pageable);
    } else {
      summaryRows = diaryRepository.searchByKeywordOrderByDateAsc(
          profileId,
          normalizedKeyword,
          normalizedDateRange.fromDate(),
          normalizedDateRange.toDate(),
          pageable);
    }

    var summaries = summaryRows.getContent().stream()
        .map(DiaryRepository.DiaryMonthlySummaryRow::toSummary)
        .toList();
    if (summaries.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, summaryRows.getTotalElements());
    }

    Map<Long, List<TagResponse>> tagsByDiaryId = diaryTagSupport.fetchAndGroupTags(extractDiaryIds(summaries));
    var responses = buildDiaryListResponses(summaries, tagsByDiaryId);
    return new PageImpl<>(responses, pageable, summaryRows.getTotalElements());
  }

  @Nullable
  private String normalizeKeyword(@Nullable String keyword) {
    if (keyword == null) {
      return null;
    }
    var trimmed = keyword.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed;
  }

  private DateRange normalizeDateRange(@Nullable LocalDate fromDate, @Nullable LocalDate toDate) {
    if (fromDate == null && toDate == null) {
      var resolvedTo = LocalDate.now();
      var resolvedFrom = resolvedTo.minusDays(SEARCH_DEFAULT_RANGE_DAYS - 1);
      return new DateRange(resolvedFrom, resolvedTo);
    }
    if (fromDate == null) {
      var resolvedTo = Objects.requireNonNull(toDate);
      return new DateRange(resolvedTo.minusDays(SEARCH_DEFAULT_RANGE_DAYS - 1), resolvedTo);
    }
    if (toDate == null) {
      return new DateRange(fromDate, LocalDate.now());
    }
    if (fromDate.isAfter(toDate)) {
      return new DateRange(toDate, fromDate);
    }
    return new DateRange(fromDate, toDate);
  }

  private List<Long> extractDiaryIds(List<DiaryMonthlySummary> diaries) {
    return diaries.stream()
        .map(DiaryMonthlySummary::id)
        .toList();
  }

  private List<DiaryListItemResponse> buildDiaryListResponses(
      List<DiaryMonthlySummary> diaries,
      Map<Long, List<TagResponse>> tagsByDiaryId) {
    return diaries.stream()
        .map(diary -> {
          List<TagResponse> tags = tagsByDiaryId.getOrDefault(diary.id(), List.of());
          return DiaryListItemResponse.from(diary, tags);
        })
        .toList();
  }

  private YearMonth resolveYearMonth(int year, int month) {
    try {
      return YearMonth.of(year, month);
    } catch (DateTimeException e) {
      return YearMonth.now();
    }
  }

  public DiaryResponse getDiary(UUID profileId, Long id) {
    var diary = findOwnedDiary(profileId, id);
    var tags = diaryTagSupport.findEmotionTagsByDiaryId(id);
    return DiaryResponse.from(diary, tags);
  }

  @Transactional
  @CacheEvict(cacheNames = {"monthlyDiaries", "emotionAnalysis"}, allEntries = true)
  public Long createDiary(UUID profileId, DiaryRequest request) {
    validateDiaryNotExists(profileId, request.date());

    Diary diary = buildDiaryFromRequest(profileId, request);
    Diary savedDiary = diaryRepository.save(diary);

    diaryTagSupport.saveDiaryTags(savedDiary.getId(), profileId, savedDiary.getDate(), request.tagIds());
    diaryYearOptionsSupport.invalidate(profileId);

    return savedDiary.getId();
  }

  private void validateDiaryNotExists(UUID profileId, LocalDate date) {
    if (diaryRepository.existsByProfileIdAndDate(profileId, date)) {
      throw new DuplicateDiaryDateException("이미 해당 날짜에 일기가 존재합니다");
    }
  }

  private Diary buildDiaryFromRequest(UUID profileId, DiaryRequest request) {
    return Diary.builder()
        .profileId(profileId)
        .date(request.date())
        .shortContent(request.shortContent())
        .situation(request.situation())
        .reaction(request.reaction())
        .physicalSensation(request.physicalSensation())
        .desiredReaction(request.desiredReaction())
        .gratitudeMoment(request.gratitudeMoment())
        .selfKindWords(request.selfKindWords())
        .imageUrl(request.imageUrl())
        .build();
  }

  @Transactional
  @CacheEvict(cacheNames = {"monthlyDiaries", "emotionAnalysis"}, allEntries = true)
  public void updateDiary(UUID profileId, Long id, DiaryRequest request) {
    var diary = findOwnedDiary(profileId, id);

    if (diaryRepository.existsByProfileIdAndDateAndIdNot(profileId, request.date(), id)) {
      throw new DuplicateDiaryDateException("이미 해당 날짜에 일기가 존재합니다");
    }

    diary.update(
        request.shortContent(),
        request.situation(),
        request.reaction(),
        request.physicalSensation(),
        request.desiredReaction(),
        request.gratitudeMoment(),
        request.selfKindWords(),
        request.imageUrl());

    diaryTagSupport.replaceDiaryTags(diary, request.tagIds());
    diaryYearOptionsSupport.invalidate(profileId);
  }

  @Transactional
  @CacheEvict(cacheNames = {"monthlyDiaries", "emotionAnalysis"}, allEntries = true)
  public void deleteDiary(UUID profileId, Long id) {
    var diary = findOwnedDiary(profileId, id);
    diaryTagSupport.deleteDiaryTagRelations(id);
    diary.softDelete();
    diaryYearOptionsSupport.invalidate(profileId);
  }

  @Transactional(readOnly = true)
  @Nullable
  public Long findIdByDate(UUID profileId, LocalDate date) {
    return diaryRepository.findByProfileIdAndDate(profileId, date)
        .map(Diary::getId)
        .orElse(null);
  }

  private Diary findOwnedDiary(UUID profileId, Long diaryId) {
    var diary = diaryRepository.findById(diaryId)
        .orElseThrow(() -> new IllegalArgumentException(DIARY_NOT_FOUND_MESSAGE));
    validateOwner(profileId, diary);
    return diary;
  }

  private void validateOwner(UUID profileId, Diary diary) {
    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException(UNAUTHORIZED_ACCESS_MESSAGE);
    }
  }

  private record DateRange(@Nullable LocalDate fromDate, @Nullable LocalDate toDate) {
  }
}
