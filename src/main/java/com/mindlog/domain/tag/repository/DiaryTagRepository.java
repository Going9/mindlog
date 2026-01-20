package com.mindlog.domain.tag.repository;

import com.mindlog.domain.tag.entity.DiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {

    List<DiaryTag> findByDiaryId(Long diaryId);

    // [추가] 일기 ID 목록(List)으로 태그들을 한 방에 조회 (WHERE diary_id IN (...))
    // N+1 문제 해결
    @Query("SELECT dt FROM DiaryTag dt JOIN FETCH dt.emotionTag WHERE dt.diaryId IN :diaryIds")
    List<DiaryTag> findAllByDiaryIdIn(@Param("diaryIds") List<Long> diaryIds);

    void deleteByDiaryId(Long diaryId);

    boolean existsByDiaryIdAndEmotionTagId(Long diaryId, Long emotionTagId);
}
