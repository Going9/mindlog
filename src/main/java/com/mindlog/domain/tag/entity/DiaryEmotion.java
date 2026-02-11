package com.mindlog.domain.tag.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "diary_emotions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"diary_id", "emotion_tag_id", "source"})
)
public class DiaryEmotion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_tag_id", nullable = false)
    private EmotionTag emotionTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_snapshot", nullable = false)
    private EmotionCategory categorySnapshot;

    @Column(name = "tag_name_snapshot", nullable = false)
    private String tagNameSnapshot;

    @Nullable
    @Column(name = "color_snapshot")
    private String colorSnapshot;

    @Column(name = "intensity", nullable = false)
    private int intensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private EmotionSource source;

    @Nullable
    @Column(name = "confidence")
    private Double confidence;

    @Builder
    public DiaryEmotion(
            Long diaryId,
            UUID profileId,
            LocalDate diaryDate,
            EmotionTag emotionTag,
            EmotionCategory categorySnapshot,
            String tagNameSnapshot,
            @Nullable String colorSnapshot,
            int intensity,
            EmotionSource source,
            @Nullable Double confidence
    ) {
        this.diaryId = diaryId;
        this.profileId = profileId;
        this.diaryDate = diaryDate;
        this.emotionTag = emotionTag;
        this.categorySnapshot = categorySnapshot;
        this.tagNameSnapshot = tagNameSnapshot;
        this.colorSnapshot = colorSnapshot;
        this.intensity = intensity;
        this.source = source;
        this.confidence = confidence;
    }

    public static DiaryEmotion fromManual(
            Long diaryId,
            UUID profileId,
            LocalDate diaryDate,
            EmotionTag emotionTag
    ) {
        return DiaryEmotion.builder()
                .diaryId(diaryId)
                .profileId(profileId)
                .diaryDate(diaryDate)
                .emotionTag(emotionTag)
                .categorySnapshot(emotionTag.getCategory())
                .tagNameSnapshot(emotionTag.getName())
                .colorSnapshot(emotionTag.getColor())
                .intensity(3)
                .source(EmotionSource.MANUAL)
                .build();
    }
}
