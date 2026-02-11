package com.mindlog.domain.tag.dto;

import com.mindlog.domain.tag.entity.EmotionTag;
import java.io.Serializable;

public record TagResponse(
    Long id,
    String name,
    String color,
    String category,
    boolean isDefault
) implements Serializable {

  // 엔티티를 DTO로 변환하는 정적 메서드
  public static TagResponse from(EmotionTag tag) {
    return new TagResponse(
        tag.getId(),
        tag.getName(),
        tag.getColor(),
        tag.getCategory().name(),
        tag.isDefault()
    );
  }
}
