package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.repository.DiaryRepository;
import com.mindlog.domain.tag.entity.DiaryTag;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.DiaryTagRepository;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

  private final DiaryRepository diaryRepository;
  private final DiaryTagRepository diaryTagRepository;
  private final EmotionTagRepository emotionTagRepository;

  public List<DiaryResponse> getMonthlyDiaries(UUID profileId, int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate start = yearMonth.atDay(1);
    LocalDate end = yearMonth.atEndOfMonth();

    // 1. 일기 조회
    List<Diary> diaries = diaryRepository.findByProfileIdAndDateBetweenOrderByDateAsc(profileId,
        start, end);

    if (diaries.isEmpty()) {
      return List.of();
    }

    // 2. ID 추출
    List<Long> diaryIds = diaries.stream()
        .map(Diary::getId)
        .toList();

    // 3. 태그 한 번에 조회 (Fetch Join으로 이미 EmotionTag가 로딩됨)
    List<DiaryTag> diaryTags = diaryTagRepository.findAllByDiaryIdIn(diaryIds);

    // 4. 그룹화 (더 이상 강제 초기화 필요 없음)
    Map<Long, List<EmotionTag>> tagsByDiaryId = diaryTags.stream()
        .collect(Collectors.groupingBy(
            DiaryTag::getDiaryId,
            Collectors.mapping(DiaryTag::getEmotionTag, Collectors.toList())
        ));

    // 5. 응답 생성
    return diaries.stream()
        .map(diary -> {
          List<EmotionTag> tags = tagsByDiaryId.getOrDefault(diary.getId(), List.of());
          return DiaryResponse.from(diary, tags);
        })
        .toList();
  }

  public DiaryResponse getDiary(UUID profileId, Long id) {
    Diary diary = diaryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException("Unauthorized access");
    }

    // 1. 태그 조회 (Fetch Join으로 쿼리 1방에 데이터 가져옴)
    List<EmotionTag> tags = diaryTagRepository.findByDiaryId(id).stream()
        .map(DiaryTag::getEmotionTag) // 이미 로딩된 상태라 get()만 하면 됨
        .toList();

    return DiaryResponse.from(diary, tags);
  }

  @Transactional
  public Long createDiary(UUID profileId, DiaryRequest request) {
    if (diaryRepository.existsByProfileIdAndDate(profileId, request.date())) {
      throw new IllegalStateException("Diary already exists for this date");
    }

    Diary diary = Diary.builder()
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

    Diary savedDiary = diaryRepository.save(diary);
    saveDiaryTags(savedDiary, request.tagIds());

    return savedDiary.getId();
  }

  @Transactional
  public void updateDiary(UUID profileId, Long id, DiaryRequest request) {
    Diary diary = diaryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

    if (!diary.getProfileId().equals(profileId)) {
      throw new IllegalArgumentException("Unauthorized access");
    }

    diary.update(
        request.shortContent(),
        request.situation(),
        request.reaction(),
        request.physicalSensation(),
        request.desiredReaction(),
        request.gratitudeMoment(),
        request.selfKindWords(),
        request.imageUrl()
    );

    // 기존 태그 삭제 후 재저장
    diaryTagRepository.deleteAllByDiaryId(id);
    saveDiaryTags(diary, request.tagIds());
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

  private void saveDiaryTags(Diary diary, List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }

    var tags = emotionTagRepository.findAllById(tagIds);

    List<DiaryTag> diaryTags = tags.stream()
        .map(tag -> {
          tag.incrementUsageCount();
          return DiaryTag.of(diary.getId(), tag);
        })
        .toList();

    diaryTagRepository.saveAll(diaryTags);
  }
}