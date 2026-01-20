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
import java.util.UUID;

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

        return diaryRepository.findByProfileIdAndDateBetweenOrderByDateAsc(profileId, start, end).stream()
                .map(DiaryResponse::from)
                .toList();
    }

    public DiaryResponse getDiary(UUID profileId, Long id) {
        Diary diary = diaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diary not found"));

        if (!diary.getProfileId().equals(profileId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        return DiaryResponse.from(diary);
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
