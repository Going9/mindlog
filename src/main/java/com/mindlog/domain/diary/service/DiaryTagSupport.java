package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.entity.DiaryEmotion;
import com.mindlog.domain.tag.entity.DiaryTag;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.DiaryEmotionRepository;
import com.mindlog.domain.tag.repository.DiaryTagRepository;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

final class DiaryTagSupport {

  private final DiaryEmotionRepository diaryEmotionRepository;
  private final DiaryTagRepository diaryTagRepository;
  private final EmotionTagRepository emotionTagRepository;

  DiaryTagSupport(
      DiaryEmotionRepository diaryEmotionRepository,
      DiaryTagRepository diaryTagRepository,
      EmotionTagRepository emotionTagRepository) {
    this.diaryEmotionRepository = diaryEmotionRepository;
    this.diaryTagRepository = diaryTagRepository;
    this.emotionTagRepository = emotionTagRepository;
  }

  Map<Long, List<TagResponse>> fetchAndGroupTags(List<Long> diaryIds) {
    if (diaryIds.isEmpty()) {
      return Map.of();
    }

    var emotionTagSummaries = diaryEmotionRepository.findTagSummaryByDiaryIds(diaryIds);
    if (emotionTagSummaries != null && !emotionTagSummaries.isEmpty()) {
      return emotionTagSummaries.stream()
          .collect(Collectors.groupingBy(
              summary -> summary.diaryId(),
              Collectors.mapping(summary -> summary.toTagResponse(), Collectors.toList())));
    }

    var diaryTagSummaries = diaryTagRepository.findTagSummaryByDiaryIds(diaryIds);
    return diaryTagSummaries.stream()
        .collect(Collectors.groupingBy(
            summary -> summary.diaryId(),
            Collectors.mapping(summary -> summary.toTagResponse(), Collectors.toList())));
  }

  void replaceDiaryTags(Diary diary, @Nullable List<Long> tagIds) {
    var diaryId = diary.getId();
    deleteDiaryTagRelations(diaryId);
    saveDiaryTags(diaryId, diary.getProfileId(), diary.getDate(), tagIds);
  }

  void saveDiaryTags(Long diaryId, UUID profileId, LocalDate diaryDate, @Nullable List<Long> tagIds) {
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

  void deleteDiaryTagRelations(Long diaryId) {
    emotionTagRepository.decrementUsageCountByDiaryId(diaryId);
    diaryTagRepository.deleteAllByDiaryId(diaryId);
    diaryEmotionRepository.deleteAllByDiaryId(diaryId);
  }

  List<EmotionTag> findEmotionTagsByDiaryId(Long diaryId) {
    var emotionRows = diaryEmotionRepository.findByDiaryId(diaryId);
    if (emotionRows != null && !emotionRows.isEmpty()) {
      return emotionRows.stream()
          .map(DiaryEmotion::getEmotionTag)
          .toList();
    }
    return diaryTagRepository.findByDiaryId(diaryId).stream()
        .map(DiaryTag::getEmotionTag)
        .toList();
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
