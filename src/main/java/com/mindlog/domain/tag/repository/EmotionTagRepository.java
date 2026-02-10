package com.mindlog.domain.tag.repository;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmotionTagRepository extends JpaRepository<EmotionTag, Long> {

    List<EmotionTag> findByProfileIdOrIsDefaultTrueOrderByUsageCountDesc(UUID profileId);

    List<EmotionTag> findByIsDefaultTrue();

    List<EmotionTag> findByProfileId(UUID profileId);

    List<EmotionTag> findByCategory(EmotionCategory category);

    Optional<EmotionTag> findByProfileIdAndName(UUID profileId, String name);

    @Query("SELECT e FROM EmotionTag e WHERE (e.profileId = :profileId OR e.isDefault = true) ORDER BY e.usageCount DESC")
    List<EmotionTag> findAvailableTagsForProfile(@Param("profileId") UUID profileId);

    @Modifying
    @Query("UPDATE EmotionTag e SET e.usageCount = e.usageCount + 1 WHERE e.id IN :tagIds")
    void incrementUsageCountByIds(@Param("tagIds") List<Long> tagIds);

    @Modifying
    @Query("""
            UPDATE EmotionTag e
            SET e.usageCount = CASE WHEN e.usageCount > 0 THEN e.usageCount - 1 ELSE 0 END
            WHERE e.id IN (
                SELECT dt.emotionTag.id
                FROM DiaryTag dt
                WHERE dt.diaryId = :diaryId
            )
            """)
    void decrementUsageCountByDiaryId(@Param("diaryId") Long diaryId);

    boolean existsByProfileIdAndName(UUID profileId, String name);
}
