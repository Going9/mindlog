-- 일기 본문 검색 성능 개선용 인덱스 (PostgreSQL / Supabase)
-- 적용 전후로 EXPLAIN ANALYZE로 실제 플랜/성능을 확인하세요.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 월 목록 조회(프로필 + 날짜 범위 + soft delete) 최적화
-- 운영 중 락 영향을 줄이기 위해 CONCURRENTLY 사용
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_diaries_profile_date_not_deleted
    ON public.diaries (profile_id, date DESC)
    WHERE is_deleted = false;

-- 다중 텍스트 컬럼 키워드 검색용 trigram 인덱스
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_diaries_search_text_trgm
    ON public.diaries
    USING gin (
        lower(
            coalesce(short_content, '') || ' ' ||
            coalesce(situation, '') || ' ' ||
            coalesce(reaction, '') || ' ' ||
            coalesce(physical_sensation, '') || ' ' ||
            coalesce(desired_reaction, '') || ' ' ||
            coalesce(gratitude_moment, '') || ' ' ||
            coalesce(self_kind_words, '')
        ) gin_trgm_ops
    )
    WHERE is_deleted = false;
