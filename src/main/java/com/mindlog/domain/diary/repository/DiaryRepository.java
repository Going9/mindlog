package com.mindlog.domain.diary.repository;

import com.mindlog.domain.diary.dto.DiaryMonthlySummary;
import com.mindlog.domain.diary.entity.Diary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByProfileIdAndDateBetweenOrderByDateAsc(UUID profileId, LocalDate start, LocalDate end);

    @Query(value = """
            SELECT
                d.id AS id,
                d.date AS date,
                d.short_content AS shortContent,
                d.situation AS situation
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.date BETWEEN :start AND :end
              AND d.is_deleted = false
            ORDER BY d.date ASC
            """, nativeQuery = true)
    List<DiaryMonthlySummaryRow> findMonthlySummaryByProfileIdAndDateBetween(
            @Param("profileId") UUID profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query(value = """
            SELECT
                d.id AS id,
                d.date AS date,
                d.short_content AS shortContent,
                d.situation AS situation
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.date BETWEEN :start AND :end
              AND d.is_deleted = false
            ORDER BY d.date DESC
            """, nativeQuery = true)
    List<DiaryMonthlySummaryRow> findMonthlySummaryByProfileIdAndDateBetweenDesc(
            @Param("profileId") UUID profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query(value = """
            SELECT
                d.id AS id,
                d.date AS date,
                d.short_content AS shortContent,
                d.situation AS situation
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.is_deleted = false
              AND d.date BETWEEN :fromDate AND :toDate
              AND lower(
                    coalesce(d.short_content, '') || ' ' ||
                    coalesce(d.situation, '') || ' ' ||
                    coalesce(d.reaction, '') || ' ' ||
                    coalesce(d.physical_sensation, '') || ' ' ||
                    coalesce(d.desired_reaction, '') || ' ' ||
                    coalesce(d.gratitude_moment, '') || ' ' ||
                    coalesce(d.self_kind_words, '')
              ) LIKE '%' || lower(cast(:keyword as text)) || '%'
            ORDER BY d.date ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.is_deleted = false
              AND d.date BETWEEN :fromDate AND :toDate
              AND lower(
                    coalesce(d.short_content, '') || ' ' ||
                    coalesce(d.situation, '') || ' ' ||
                    coalesce(d.reaction, '') || ' ' ||
                    coalesce(d.physical_sensation, '') || ' ' ||
                    coalesce(d.desired_reaction, '') || ' ' ||
                    coalesce(d.gratitude_moment, '') || ' ' ||
                    coalesce(d.self_kind_words, '')
              ) LIKE '%' || lower(cast(:keyword as text)) || '%'
            """, nativeQuery = true)
    Page<DiaryMonthlySummaryRow> searchByKeywordOrderByDateAsc(
            @Param("profileId") UUID profileId,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query(value = """
            SELECT
                d.id AS id,
                d.date AS date,
                d.short_content AS shortContent,
                d.situation AS situation
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.is_deleted = false
              AND d.date BETWEEN :fromDate AND :toDate
              AND lower(
                    coalesce(d.short_content, '') || ' ' ||
                    coalesce(d.situation, '') || ' ' ||
                    coalesce(d.reaction, '') || ' ' ||
                    coalesce(d.physical_sensation, '') || ' ' ||
                    coalesce(d.desired_reaction, '') || ' ' ||
                    coalesce(d.gratitude_moment, '') || ' ' ||
                    coalesce(d.self_kind_words, '')
              ) LIKE '%' || lower(cast(:keyword as text)) || '%'
            ORDER BY d.date DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM public.diaries d
            WHERE d.profile_id = :profileId
              AND d.is_deleted = false
              AND d.date BETWEEN :fromDate AND :toDate
              AND lower(
                    coalesce(d.short_content, '') || ' ' ||
                    coalesce(d.situation, '') || ' ' ||
                    coalesce(d.reaction, '') || ' ' ||
                    coalesce(d.physical_sensation, '') || ' ' ||
                    coalesce(d.desired_reaction, '') || ' ' ||
                    coalesce(d.gratitude_moment, '') || ' ' ||
                    coalesce(d.self_kind_words, '')
              ) LIKE '%' || lower(cast(:keyword as text)) || '%'
            """, nativeQuery = true)
    Page<DiaryMonthlySummaryRow> searchByKeywordOrderByDateDesc(
            @Param("profileId") UUID profileId,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Nullable
    @Query("""
            SELECT
              MIN(d.date) AS minDate,
              MAX(d.date) AS maxDate
            FROM Diary d
            WHERE d.profileId = :profileId
            """)
    DateRangeView findDateRangeByProfileId(@Param("profileId") UUID profileId);

    Optional<Diary> findByProfileIdAndDate(UUID profileId, LocalDate date);
    boolean existsByProfileIdAndDate(UUID profileId, LocalDate date);
    boolean existsByProfileIdAndDateAndIdNot(UUID profileId, LocalDate date, Long id);

    interface DateRangeView {
        @Nullable
        LocalDate getMinDate();

        @Nullable
        LocalDate getMaxDate();
    }

    interface DiaryMonthlySummaryRow {
        Long getId();

        LocalDate getDate();

        @Nullable
        String getShortContent();

        @Nullable
        String getSituation();

        default DiaryMonthlySummary toSummary() {
            return new DiaryMonthlySummary(getId(), getDate(), getShortContent(), getSituation());
        }
    }
}
