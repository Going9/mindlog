package com.mindlog.domain.diary.repository;

import com.mindlog.domain.diary.dto.DiaryMonthlySummary;
import com.mindlog.domain.diary.entity.Diary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByProfileIdAndDateBetweenOrderByDateAsc(UUID profileId, LocalDate start, LocalDate end);

    @Query("""
            SELECT new com.mindlog.domain.diary.dto.DiaryMonthlySummary(
                d.id,
                d.date,
                d.shortContent,
                d.situation
            )
            FROM Diary d
            WHERE d.profileId = :profileId
              AND d.date BETWEEN :start AND :end
            ORDER BY d.date ASC
            """)
    List<DiaryMonthlySummary> findMonthlySummaryByProfileIdAndDateBetween(
            @Param("profileId") UUID profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT new com.mindlog.domain.diary.dto.DiaryMonthlySummary(
                d.id,
                d.date,
                d.shortContent,
                d.situation
            )
            FROM Diary d
            WHERE d.profileId = :profileId
              AND d.date BETWEEN :start AND :end
            ORDER BY d.date DESC
            """)
    List<DiaryMonthlySummary> findMonthlySummaryByProfileIdAndDateBetweenDesc(
            @Param("profileId") UUID profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Nullable
    @Query("SELECT MIN(d.date) FROM Diary d WHERE d.profileId = :profileId")
    LocalDate findMinDateByProfileId(@Param("profileId") UUID profileId);

    @Nullable
    @Query("SELECT MAX(d.date) FROM Diary d WHERE d.profileId = :profileId")
    LocalDate findMaxDateByProfileId(@Param("profileId") UUID profileId);

    Optional<Diary> findByProfileIdAndDate(UUID profileId, LocalDate date);
    boolean existsByProfileIdAndDate(UUID profileId, LocalDate date);
    boolean existsByProfileIdAndDateAndIdNot(UUID profileId, LocalDate date, Long id);
}
