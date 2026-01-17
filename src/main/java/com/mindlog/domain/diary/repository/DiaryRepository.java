package com.mindlog.domain.diary.repository;

import com.mindlog.domain.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByProfileIdAndDateBetweenOrderByDateAsc(UUID profileId, LocalDate start, LocalDate end);
    Optional<Diary> findByProfileIdAndDate(UUID profileId, LocalDate date);
    boolean existsByProfileIdAndDate(UUID profileId, LocalDate date);
}
