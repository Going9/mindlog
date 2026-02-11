package com.mindlog.domain.tag.repository;

import com.mindlog.domain.tag.entity.DiaryEmotion;
import com.mindlog.domain.tag.entity.EmotionCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, Long> {

    @Query("SELECT de FROM DiaryEmotion de JOIN FETCH de.emotionTag WHERE de.diaryId = :diaryId")
    List<DiaryEmotion> findByDiaryId(@Param("diaryId") Long diaryId);

    @Query("SELECT de FROM DiaryEmotion de JOIN FETCH de.emotionTag WHERE de.diaryId IN :diaryIds")
    List<DiaryEmotion> findAllByDiaryIdIn(@Param("diaryIds") List<Long> diaryIds);

    @Modifying
    @Query("DELETE FROM DiaryEmotion de WHERE de.diaryId = :diaryId")
    void deleteAllByDiaryId(@Param("diaryId") Long diaryId);

    @Query("""
            SELECT
                de.categorySnapshot AS category,
                COUNT(de) AS count
            FROM DiaryEmotion de
            WHERE de.profileId = :profileId
              AND de.diaryDate BETWEEN :fromDate AND :toDate
            GROUP BY de.categorySnapshot
            """)
    List<CategoryCountView> countByCategoryInRange(
            @Param("profileId") UUID profileId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT
                de.emotionTag.id AS tagId,
                de.tagNameSnapshot AS tagName,
                de.colorSnapshot AS color,
                de.categorySnapshot AS category,
                COUNT(de) AS count
            FROM DiaryEmotion de
            WHERE de.profileId = :profileId
              AND de.diaryDate BETWEEN :fromDate AND :toDate
            GROUP BY de.emotionTag.id, de.tagNameSnapshot, de.colorSnapshot, de.categorySnapshot
            ORDER BY COUNT(de) DESC, de.tagNameSnapshot ASC
            """)
    List<TagCountView> findTopTagsInRange(
            @Param("profileId") UUID profileId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("""
            SELECT
                de.diaryDate AS date,
                SUM(CASE WHEN de.categorySnapshot = :positive THEN 1 ELSE 0 END) AS positiveCount,
                SUM(CASE WHEN de.categorySnapshot = :negative THEN 1 ELSE 0 END) AS negativeCount,
                SUM(CASE WHEN de.categorySnapshot = :neutral THEN 1 ELSE 0 END) AS neutralCount,
                COUNT(de) AS totalCount,
                AVG(de.intensity) AS avgIntensity
            FROM DiaryEmotion de
            WHERE de.profileId = :profileId
              AND de.diaryDate BETWEEN :fromDate AND :toDate
            GROUP BY de.diaryDate
            ORDER BY de.diaryDate ASC
            """)
    List<DailyTrendView> findDailyTrendInRange(
            @Param("profileId") UUID profileId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("positive") EmotionCategory positive,
            @Param("negative") EmotionCategory negative,
            @Param("neutral") EmotionCategory neutral);

    interface CategoryCountView {
        EmotionCategory getCategory();
        Long getCount();
    }

    interface TagCountView {
        Long getTagId();
        String getTagName();
        String getColor();
        EmotionCategory getCategory();
        Long getCount();
    }

    interface DailyTrendView {
        LocalDate getDate();
        Long getPositiveCount();
        Long getNegativeCount();
        Long getNeutralCount();
        Long getTotalCount();
        Double getAvgIntensity();
    }
}
