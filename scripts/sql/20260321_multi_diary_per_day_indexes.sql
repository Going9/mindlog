-- 같은 날짜 다건 일기 허용을 위한 조회 인덱스 조정 스크립트 (PostgreSQL / Supabase)
-- 운영 반영 전 실제 제약/인덱스 이름을 반드시 확인하세요.
--
-- 확인 예시:
-- SELECT conname, pg_get_constraintdef(oid)
-- FROM pg_constraint
-- WHERE conrelid = 'public.diaries'::regclass;
--
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE schemaname = 'public'
--   AND tablename = 'diaries';
--
-- (profile_id, date) 유니크 제약/인덱스가 실제 운영 DB에 있다면
-- 위 조회 결과의 이름으로 먼저 DROP 해야 같은 날짜 다건 저장이 가능합니다.

DROP INDEX CONCURRENTLY IF EXISTS idx_diaries_profile_date_not_deleted;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_diaries_profile_date_created_id_not_deleted
    ON public.diaries (profile_id, date DESC, created_at DESC, id DESC)
    WHERE is_deleted = false;
