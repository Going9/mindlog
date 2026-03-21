package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.dto.DiaryWriteAllowance;
import com.mindlog.domain.diary.repository.DiaryRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryWritePolicyService {

  private final DiaryRepository diaryRepository;

  public DiaryWriteAllowance getAllowance(UUID profileId, LocalDate date) {
    var usedCount = diaryRepository.countActiveByProfileIdAndDate(profileId, date);
    return DiaryWriteAllowance.unlimited(date, usedCount);
  }
}
