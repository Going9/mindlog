package com.mindlog.domain.tag.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "diary_tags")
public class DiaryTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_tag_id", nullable = false)
    private EmotionTag emotionTag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DiaryTag(Long diaryId, EmotionTag emotionTag) {
        this.diaryId = diaryId;
        this.emotionTag = emotionTag;
    }

    public static DiaryTag of(Long diaryId, EmotionTag emotionTag) {
        return new DiaryTag(diaryId, emotionTag);
    }
}
