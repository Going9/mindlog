-- 감정 분석 확장을 위한 diary_emotions 도입
-- 실행 대상: Supabase PostgreSQL (public schema)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'emotion_source'
          AND n.nspname = 'public'
    ) THEN
        CREATE TYPE public.emotion_source AS ENUM ('MANUAL', 'AI');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS public.diary_emotions (
    id BIGSERIAL PRIMARY KEY,
    diary_id BIGINT NOT NULL,
    profile_id UUID NOT NULL,
    diary_date DATE NOT NULL,
    emotion_tag_id BIGINT NOT NULL,
    category_snapshot public.emotion_category NOT NULL,
    tag_name_snapshot TEXT NOT NULL,
    color_snapshot TEXT,
    intensity INTEGER NOT NULL DEFAULT 3,
    source public.emotion_source NOT NULL DEFAULT 'MANUAL',
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT diary_emotions_diary_id_fkey
        FOREIGN KEY (diary_id) REFERENCES public.diaries(id) ON DELETE CASCADE,
    CONSTRAINT diary_emotions_emotion_tag_id_fkey
        FOREIGN KEY (emotion_tag_id) REFERENCES public.emotion_tags(id) ON DELETE CASCADE,
    CONSTRAINT diary_emotions_intensity_check
        CHECK (intensity BETWEEN 1 AND 5),
    CONSTRAINT diary_emotions_diary_emotion_source_unique
        UNIQUE (diary_id, emotion_tag_id, source)
);

CREATE INDEX IF NOT EXISTS idx_diary_emotions_diary_id
    ON public.diary_emotions(diary_id);
CREATE INDEX IF NOT EXISTS idx_diary_emotions_emotion_tag_id
    ON public.diary_emotions(emotion_tag_id);
CREATE INDEX IF NOT EXISTS idx_diary_emotions_profile_date
    ON public.diary_emotions(profile_id, diary_date);
CREATE INDEX IF NOT EXISTS idx_diary_emotions_profile_category_date
    ON public.diary_emotions(profile_id, category_snapshot, diary_date);

COMMENT ON TABLE public.diary_emotions IS '감정 분석을 위한 일기 감정 기록 테이블';

-- 기존 diary_tags 데이터를 신규 구조로 백필
INSERT INTO public.diary_emotions (
    diary_id,
    profile_id,
    diary_date,
    emotion_tag_id,
    category_snapshot,
    tag_name_snapshot,
    color_snapshot,
    intensity,
    source,
    confidence
)
SELECT
    dt.diary_id,
    d.profile_id,
    d.date,
    dt.emotion_tag_id,
    COALESCE(et.category, 'NEUTRAL'::public.emotion_category),
    et.name,
    et.color,
    3,
    'MANUAL'::public.emotion_source,
    NULL
FROM public.diary_tags dt
JOIN public.diaries d ON d.id = dt.diary_id
JOIN public.emotion_tags et ON et.id = dt.emotion_tag_id
ON CONFLICT (diary_id, emotion_tag_id, source) DO NOTHING;
