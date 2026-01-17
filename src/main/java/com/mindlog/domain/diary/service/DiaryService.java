package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.repository.DiaryRepository;
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
        // Ensure only one diary per day
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

        return diaryRepository.save(diary).getId();
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
}
