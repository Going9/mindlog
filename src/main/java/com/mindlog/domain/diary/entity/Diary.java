package com.mindlog.domain.diary.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "diaries")
@SQLRestriction("is_deleted = false")
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Nullable
    @Column(name = "short_content", columnDefinition = "TEXT")
    private String shortContent;

    @Nullable
    @Column(name = "situation", columnDefinition = "TEXT")
    private String situation;

    @Nullable
    @Column(name = "reaction", columnDefinition = "TEXT")
    private String reaction;

    @Nullable
    @Column(name = "physical_sensation", columnDefinition = "TEXT")
    private String physicalSensation;

    @Nullable
    @Column(name = "desired_reaction", columnDefinition = "TEXT")
    private String desiredReaction;

    @Nullable
    @Column(name = "gratitude_moment", columnDefinition = "TEXT")
    private String gratitudeMoment;

    @Nullable
    @Column(name = "self_kind_words", columnDefinition = "TEXT")
    private String selfKindWords;

    @Nullable
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public Diary(
            UUID profileId,
            LocalDate date,
            @Nullable String shortContent,
            @Nullable String situation,
            @Nullable String reaction,
            @Nullable String physicalSensation,
            @Nullable String desiredReaction,
            @Nullable String gratitudeMoment,
            @Nullable String selfKindWords,
            @Nullable String imageUrl
    ) {
        this.profileId = profileId;
        this.date = date;
        this.shortContent = shortContent;
        this.situation = situation;
        this.reaction = reaction;
        this.physicalSensation = physicalSensation;
        this.desiredReaction = desiredReaction;
        this.gratitudeMoment = gratitudeMoment;
        this.selfKindWords = selfKindWords;
        this.imageUrl = imageUrl;
        this.isDeleted = false;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
