-- 같은 날짜 다건 일기 허용을 위한 제약/인덱스 정리 스크립트 (PostgreSQL / Supabase)
-- 주의:
-- 1. UNIQUE(profile_id, date) 제약/인덱스가 남아 있으면 다건 저장이 계속 실패합니다.
-- 2. 아래 DROP/CREATE는 잠시 잠금을 유발할 수 있으므로 저트래픽 시간대 실행을 권장합니다.

-- 1) UNIQUE(profile_id, date) 제약 제거
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN LATERAL (
            SELECT array_agg(att.attname ORDER BY key_col.ordinality) AS column_names
            FROM unnest(con.conkey) WITH ORDINALITY AS key_col(attnum, ordinality)
            JOIN pg_attribute att
              ON att.attrelid = con.conrelid
             AND att.attnum = key_col.attnum
        ) cols ON true
        WHERE con.conrelid = 'public.diaries'::regclass
          AND con.contype = 'u'
          AND cols.column_names = ARRAY['profile_id', 'date']
    LOOP
        RAISE NOTICE 'Dropping unique constraint: %', constraint_name;
        EXECUTE format('ALTER TABLE public.diaries DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

-- 2) 제약에 종속되지 않은 UNIQUE(profile_id, date...) 인덱스 제거
DO $$
DECLARE
    index_name text;
BEGIN
    FOR index_name IN
        SELECT idx.relname
        FROM pg_index i
        JOIN pg_class idx
          ON idx.oid = i.indexrelid
        WHERE i.indrelid = 'public.diaries'::regclass
          AND i.indisunique = true
          AND NOT EXISTS (
              SELECT 1
              FROM pg_constraint con
              WHERE con.conindid = i.indexrelid
          )
          AND pg_get_indexdef(i.indexrelid) LIKE
              'CREATE UNIQUE INDEX % ON public.diaries USING btree (profile_id, date%'
    LOOP
        RAISE NOTICE 'Dropping unique index: %', index_name;
        EXECUTE format('DROP INDEX IF EXISTS public.%I', index_name);
    END LOOP;
END $$;

-- 3) 기존 조회용 인덱스를 다건 정렬 기준에 맞게 교체
DROP INDEX IF EXISTS public.idx_diaries_profile_date_not_deleted;

CREATE INDEX IF NOT EXISTS idx_diaries_profile_date_created_id_not_deleted
    ON public.diaries (profile_id, date DESC, created_at DESC, id DESC)
    WHERE is_deleted = false;
