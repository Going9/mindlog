-- emotion_tags 조회 최적화 인덱스
-- 대상 쿼리: (profile_id = ? OR is_default = true) ORDER BY usage_count DESC

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_emotion_tags_profile_usage_desc
    ON public.emotion_tags (profile_id, usage_count DESC)
    WHERE profile_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_emotion_tags_default_usage_desc
    ON public.emotion_tags (usage_count DESC)
    WHERE is_default = true;
