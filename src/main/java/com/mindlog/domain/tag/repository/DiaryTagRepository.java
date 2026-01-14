package com.mindlog.domain.tag.repository;

import com.mindlog.domain.tag.entity.DiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {

    List<DiaryTag> findByDiaryId(Long diaryId);

    void deleteByDiaryId(Long diaryId);

    boolean existsByDiaryIdAndEmotionTagId(Long diaryId, Long emotionTagId);
}
