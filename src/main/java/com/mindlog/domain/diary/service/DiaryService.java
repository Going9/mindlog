package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.repository.DiaryRepository;
import com.mindlog.domain.tag.entity.DiaryTag;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.repository.DiaryTagRepository;
import com.mindlog.domain.tag.repository.EmotionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // 1. 이번 달 일기들을 모두 가져옴
        List<Diary> diaries = diaryRepository.findByProfileIdAndDateBetweenOrderByDateAsc(profileId, start, end);

        if (diaries.isEmpty()) {
            return List.of();
        }

        // 2. 일기들의 ID만 리스트로 추출
        List<Long> diaryIds = diaries.stream()
                .map(Diary::getId)
                .toList();

        // 3. 이 일기들에 붙은 모든 태그를 '한 방에' 조회 (쿼리 1번)
        List<DiaryTag> diaryTags = diaryTagRepository.findAllByDiaryIdIn(diaryIds);

        // 4. 태그들을 "일기장 ID" 별로 그룹화 (Map<Long, List<EmotionTag>>)
        // 결과 예시: { 1번일기: [기쁨, 슬픔], 2번일기: [불안] ... }
        Map<Long, List<EmotionTag>> tagsByDiaryId = diaryTags.stream()
                .collect(Collectors.groupingBy(
                        DiaryTag::getDiaryId,
                        Collectors.mapping(DiaryTag::getEmotionTag, Collectors.toList())
                ));

        // 5. 일기와 태그를 합쳐서 응답 생성
        return diaries.stream()
                .map(diary -> {
                    // 이 일기에 해당하는 태그 리스트를 꺼냄 (없으면 빈 리스트)
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

        // 1. 해당 일기에 붙은 태그 목록 조회 (중계 테이블 -> 감정 태그 추출)
        List<EmotionTag> tags = diaryTagRepository.findByDiaryId(id).stream()
                .map(DiaryTag::getEmotionTag)
                .toList();

        // 2. from 메서드에 tags 리스트도 같이 전달
        return DiaryResponse.from(diary, tags);
    }

    @Transactional
    public Long createDiary(UUID profileId, DiaryRequest request) {
        // 1. 날짜 중복 체크
        if (diaryRepository.existsByProfileIdAndDate(profileId, request.date())) {
            throw new IllegalStateException("Diary already exists for this date");
        }

        // 2. 일기 엔티티 생성
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

        // 1. 일기 내용 업데이트
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

        // 2. 태그 업데이트
        diaryTagRepository.deleteByDiaryId(id); // 기존 태그 전부 삭제 후
        saveDiaryTags(diary, request.tagIds()); // 저장
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

    // 태그 저장 헬퍼 메서드
    private void saveDiaryTags(Diary diary, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;

        // 선택된 태그 ID로 실제 태그 객체들을 조회 (한 번에 조회해서 성능 최적화)
        var tags = emotionTagRepository.findAllById(tagIds);

        // DiaryTag 리스트로 변환
        List<DiaryTag> diaryTags = tags.stream()
                .map(tag -> {
                    tag.incrementUsageCount();
                    return DiaryTag.of(diary.getId(), tag);
                })
                .toList();

        // 한 번에 저장 (Bulk Insert)
        diaryTagRepository.saveAll(diaryTags);
    }
}
