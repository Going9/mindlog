package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryListItemResponse;
import com.mindlog.domain.diary.dto.DiaryMonthlySummary;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.exception.DuplicateDiaryDateException;
import com.mindlog.domain.diary.repository.DiaryRepository;
import com.mindlog.domain.tag.entity.DiaryEmotion;
import com.mindlog.domain.tag.entity.DiaryTag;
import com.mindlog.domain.tag.entity.EmotionTag;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

  private final DiaryRepository diaryRepository;
  private final DiaryEmotionRepository diaryEmotionRepository;
  private final DiaryTagRepository diaryTagRepository;
  private final EmotionTagRepository emotionTagRepository;

  public List<DiaryListItemResponse> getMonthlyDiaries(UUID profileId, int year, int month, boolean newestFirst) {
    YearMonth yearMonth = resolveYearMonth(year, month);
    List<DiaryMonthlySummary> diaries = findDiariesByMonth(profileId, yearMonth, newestFirst);
    if (diaries.isEmpty()) {
      return List.of();
    }

    Map<Long, List<EmotionTag>> tagsByDiaryId = fetchAndGroupTags(extractDiaryIds(diaries));

    return buildDiaryListResponses(diaries, tagsByDiaryId);
  }

  public List<Integer> getAvailableYears(UUID profileId, int selectedYear) {
    LocalDate minDate = diaryRepository.findMinDateByProfileId(profileId);
    LocalDate maxDate = diaryRepository.findMaxDateByProfileId(profileId);

    if (minDate == null || maxDate == null) {
      return IntStream.rangeClosed(selectedYear - 2, selectedYear + 2)
          .boxed()
          .sorted((a, b) -> Integer.compare(b, a))
          .toList();
    }

    int startYear = Math.min(minDate.getYear(), selectedYear);
    int endYear = Math.max(maxDate.getYear(), selectedYear);

    return IntStream.rangeClosed(startYear, endYear)
        .boxed()
        .sorted((a, b) -> Integer.compare(b, a))
        .toList();
  }

  private List<DiaryMonthlySummary> findDiariesByMonth(UUID profileId, YearMonth yearMonth, boolean newestFirst) {
    LocalDate start = yearMonth.atDay(1);
    LocalDate end = yearMonth.atEndOfMonth();
    if (newestFirst) {
      return diaryRepository.findMonthlySummaryByProfileIdAndDateBetweenDesc(profileId, start, end);
    }
    return diaryRepository.findMonthlySummaryByProfileIdAndDateBetween(profileId, start, end);
  }

  private Map<Long, List<EmotionTag>> fetchAndGroupTags(List<Long> diaryIds) {
    List<DiaryEmotion> diaryEmotions = diaryEmotionRepository.findAllByDiaryIdIn(diaryIds);
    if (diaryEmotions != null && !diaryEmotions.isEmpty()) {
      return diaryEmotions.stream()
          .collect(Collectors.groupingBy(
              DiaryEmotion::getDiaryId,
              Collectors.mapping(DiaryEmotion::getEmotionTag, Collectors.toList())));
    }

    List<DiaryTag> diaryTags = diaryTagRepository.findAllByDiaryIdIn(diaryIds);

    return diaryTags.stream()
        .collect(Collectors.groupingBy(
            DiaryTag::getDiaryId,
            Collectors.mapping(DiaryTag::getEmotionTag, Collectors.toList())));
  }

  private List<Long> extractDiaryIds(List<DiaryMonthlySummary> diaries) {
    return diaries.stream()
        .map(DiaryMonthlySummary::id)
        .toList();
  }

  private List<DiaryListItemResponse> buildDiaryListResponses(
      List<DiaryMonthlySummary> diaries,
      Map<Long, List<EmotionTag>> tagsByDiaryId) {
    return diaries.stream()
        .map(diary -> {
          List<EmotionTag> tags = tagsByDiaryId.getOrDefault(diary.id(), List.of());
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
    Diary diary = diaryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException("Unauthorized access");
    }

    // 1. 태그 조회 (Fetch Join으로 쿼리 1방에 데이터 가져옴)
    var emotionRows = diaryEmotionRepository.findByDiaryId(id);
    List<EmotionTag> tags;
    if (emotionRows != null && !emotionRows.isEmpty()) {
      tags = emotionRows.stream()
          .map(DiaryEmotion::getEmotionTag)
          .toList();
    } else {
      tags = diaryTagRepository.findByDiaryId(id).stream()
          .map(DiaryTag::getEmotionTag)
          .toList();
    }

    return DiaryResponse.from(diary, tags);
  }

  @Transactional
  public Long createDiary(UUID profileId, DiaryRequest request) {
    validateDiaryNotExists(profileId, request.date());

    Diary diary = buildDiaryFromRequest(profileId, request);
    Diary savedDiary = diaryRepository.save(diary);

    saveDiaryTags(savedDiary.getId(), profileId, savedDiary.getDate(), request.tagIds());

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
  public void updateDiary(UUID profileId, Long id, DiaryRequest request) {
    Diary diary = diaryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException("Unauthorized access");
    }

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

    replaceDiaryTags(diary, request.tagIds());
  }

  @Transactional
  public void deleteDiary(UUID profileId, Long id) {
    Diary diary = diaryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException("Unauthorized access");
    }

    diary.softDelete();
  }

  @Transactional(readOnly = true)
  @Nullable
  public Long findIdByDate(UUID profileId, LocalDate date) {
    return diaryRepository.findByProfileIdAndDate(profileId, date)
        .map(Diary::getId)
        .orElse(null);
  }

  private void replaceDiaryTags(Diary diary, @Nullable List<Long> tagIds) {
    var diaryId = diary.getId();
    emotionTagRepository.decrementUsageCountByDiaryId(diaryId);
    diaryTagRepository.deleteAllByDiaryId(diaryId);
    diaryEmotionRepository.deleteAllByDiaryId(diaryId);
    saveDiaryTags(diaryId, diary.getProfileId(), diary.getDate(), tagIds);
  }

  private void saveDiaryTags(Long diaryId, UUID profileId, LocalDate diaryDate, @Nullable List<Long> tagIds) {
    List<Long> normalizedTagIds = normalizeTagIds(tagIds);
    if (normalizedTagIds.isEmpty()) {
      return;
    }

    emotionTagRepository.incrementUsageCountByIds(normalizedTagIds);
    List<EmotionTag> resolvedTags = resolveEmotionTags(normalizedTagIds);

    List<DiaryTag> diaryTags = resolvedTags.stream()
        .map(tag -> DiaryTag.of(diaryId, tag))
        .toList();

    List<DiaryEmotion> diaryEmotions = resolvedTags.stream()
        .map(tag -> DiaryEmotion.fromManual(diaryId, profileId, diaryDate, tag))
        .toList();

    diaryTagRepository.saveAll(diaryTags);
    diaryEmotionRepository.saveAll(diaryEmotions);
  }

  private List<EmotionTag> resolveEmotionTags(List<Long> normalizedTagIds) {
    var fetchedTags = emotionTagRepository.findAllById(normalizedTagIds);
    var tagsById = fetchedTags.stream()
        .filter(tag -> tag.getId() != null)
        .collect(Collectors.toMap(EmotionTag::getId, tag -> tag));

    if (tagsById.size() == normalizedTagIds.size()) {
      return normalizedTagIds.stream()
          .map(tagsById::get)
          .filter(Objects::nonNull)
          .toList();
    }

    // 테스트 더블처럼 id가 없는 엔티티를 반환하는 경우를 대비한 fallback.
    if (fetchedTags.size() == normalizedTagIds.size()) {
      return fetchedTags;
    }

    throw new IllegalStateException("일부 감정 태그를 찾을 수 없습니다.");
  }

  private List<Long> normalizeTagIds(@Nullable List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return List.of();
    }
    return tagIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
