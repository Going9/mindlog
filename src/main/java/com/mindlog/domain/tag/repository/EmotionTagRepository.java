package com.mindlog.domain.tag.repository;

import com.mindlog.domain.tag.entity.EmotionCategory;
import com.mindlog.domain.tag.entity.EmotionTag;
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

    boolean existsByProfileIdAndName(UUID profileId, String name);
}
