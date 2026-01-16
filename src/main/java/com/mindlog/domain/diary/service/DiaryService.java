package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.entity.Diary;
import com.mindlog.domain.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;

    @Transactional
    public DiaryResponse createDiary(UUID profileId, DiaryRequest request) {
        // 이미 같은 날짜에 일기가 있는지 확인 (옵션)
        if (diaryRepository.existsByProfileIdAndDate(profileId, request.date())) {
            throw new IllegalStateException("해당 날짜에 이미 일기가 존재합니다.");
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
        return DiaryResponse.from(savedDiary);
    }

    public List<DiaryResponse> getDiaries(UUID profileId) {
        return diaryRepository.findByProfileIdOrderByDateDesc(profileId)
                .stream()
                .map(DiaryResponse::from)
                .toList();
    }

    public DiaryResponse getDiary(UUID profileId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        if (!diary.getProfileId().equals(profileId)) {
            throw new SecurityException("자신의 일기만 조회할 수 있습니다.");
        }

        return DiaryResponse.from(diary);
    }

    @Transactional
    public void deleteDiary(UUID profileId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        if (!diary.getProfileId().equals(profileId)) {
            throw new SecurityException("자신의 일기만 삭제할 수 있습니다.");
        }

        diary.softDelete();
    }
}
