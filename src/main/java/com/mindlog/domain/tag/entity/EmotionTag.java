package com.mindlog.domain.tag.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * 감정 태그 엔티티.
 *
 * <p>기본 태그(profile_id가 null)와 사용자 커스텀 태그(profile_id가 존재)를 모두 관리한다.</p>
 *
 * <h3>데이터 무결성 제약 조건</h3>
 * <ul>
 *   <li><b>UNIQUE(profile_id, name)</b>: 동일 사용자 내에서 태그명 중복 방지</li>
 *   <li><b>Partial Index (DB 레벨)</b>: PostgreSQL의 UNIQUE 제약은 NULL을 서로 다른 값으로 취급하므로,
 *       기본 태그(profile_id IS NULL)의 이름 중복을 방지하기 위해 별도의 부분 인덱스가 필요함.
 *       <pre>CREATE UNIQUE INDEX idx_unique_default_tag_name ON emotion_tags(name) WHERE profile_id IS NULL;</pre>
 *   </li>
 * </ul>
 *
 * @see com.mindlog.domain.tag.repository.EmotionTagRepository
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "emotion_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "name"})
)
public class EmotionTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Nullable
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "name", nullable = false)
    private String name;

    @Nullable
    @Column(name = "color")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private EmotionCategory category;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Builder
    public EmotionTag(
            @Nullable UUID profileId,
            String name,
            @Nullable String color,
            EmotionCategory category,
            boolean isDefault
    ) {
        this.profileId = profileId;
        this.name = name;
        this.color = color;
        this.category = category;
        this.isDefault = isDefault;
        this.usageCount = 0;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }
}
