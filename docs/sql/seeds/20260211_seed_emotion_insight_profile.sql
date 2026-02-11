-- 감정 인사이트 테스트용 더미 데이터 시드
-- 대상 프로필: eb2d1a56-1f10-4d11-8477-db397929b9a1
-- 정책:
-- 1) 기존 데이터는 삭제하지 않는다.
-- 2) 지정 기간(2025-12-15 ~ 2026-02-11)에서 "없는 날짜만" 일기 생성한다.
-- 3) 생성한 일기에 대해 diary_emotions + diary_tags를 함께 기록한다.
-- 4) 해당 프로필의 커스텀 태그 usage_count를 실제 연결 수로 재계산한다.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.profiles p
        WHERE p.id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
    ) THEN
        RAISE EXCEPTION 'profile not found: %', 'eb2d1a56-1f10-4d11-8477-db397929b9a1';
    END IF;
END$$;

-- 1) 인사이트 테스트용 커스텀 태그 보강 (없으면 생성, 있으면 색/카테고리 보정)
INSERT INTO public.emotion_tags (profile_id, name, color, category, is_default, usage_count, created_at, updated_at)
VALUES
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '행복', '#f59e0b', 'POSITIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '감사', '#fbbf24', 'POSITIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '평온', '#10b981', 'POSITIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '뿌듯', '#84cc16', 'POSITIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '희망', '#22c55e', 'POSITIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '불안', '#ef4444', 'NEGATIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '초조', '#f97316', 'NEGATIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '지침', '#a855f7', 'NEGATIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '답답', '#e11d48', 'NEGATIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '슬픔', '#64748b', 'NEGATIVE', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '무난', '#6b7280', 'NEUTRAL', false, 0, now(), now()),
    ('eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid, '집중', '#0ea5e9', 'NEUTRAL', false, 0, now(), now())
ON CONFLICT (profile_id, name) DO UPDATE
SET color = EXCLUDED.color,
    category = EXCLUDED.category,
    updated_at = now();

-- 2) 생성 대상 날짜 테이블
CREATE TEMP TABLE _seed_dates AS
SELECT generate_series('2025-12-15'::date, '2026-02-11'::date, '1 day'::interval)::date AS diary_date;

-- 3) 없는 날짜만 일기 생성
CREATE TEMP TABLE _new_diaries AS
WITH inserted AS (
    INSERT INTO public.diaries (
        profile_id,
        date,
        short_content,
        situation,
        reaction,
        physical_sensation,
        desired_reaction,
        gratitude_moment,
        self_kind_words,
        image_url,
        is_deleted,
        created_at,
        updated_at
    )
    SELECT
        'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid,
        d.diary_date,
        CASE
            WHEN ((d.diary_date - '2025-12-15'::date) % 6) = 0 THEN '작은 성취가 있었던 날'
            WHEN ((d.diary_date - '2025-12-15'::date) % 6) = 1 THEN '업무 압박이 컸던 날'
            WHEN ((d.diary_date - '2025-12-15'::date) % 6) = 2 THEN '사람들과 대화가 많았던 날'
            WHEN ((d.diary_date - '2025-12-15'::date) % 6) = 3 THEN '에너지가 떨어진 날'
            WHEN ((d.diary_date - '2025-12-15'::date) % 6) = 4 THEN '기분이 차분해진 날'
            ELSE '조금 복합적인 하루'
        END,
        '상황 기록: ' || to_char(d.diary_date, 'YYYY-MM-DD') || '의 주요 사건을 정리했다.',
        '반응 기록: 감정을 알아차리고 메모를 남겼다.',
        '신체 감각: 어깨 긴장, 호흡 속도 변화를 관찰했다.',
        '원했던 반응: 즉각 반응보다 한 박자 쉬는 선택.',
        '감사한 순간: 사소한 도움과 따뜻한 말.',
        '나에게: 완벽하지 않아도 충분히 잘하고 있다.',
        NULL,
        false,
        now(),
        now()
    FROM _seed_dates d
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.diaries x
        WHERE x.profile_id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
          AND x.date = d.diary_date
          AND x.is_deleted = false
    )
    RETURNING id, date
)
SELECT id, date FROM inserted;

-- 4) 태그 매핑용 뷰 (이번 시드에서 생성한 일기만 대상으로 한다)
CREATE TEMP TABLE _new_diary_base AS
SELECT
    nd.id AS diary_id,
    nd.date AS diary_date,
    (nd.date - '2025-12-15'::date)::int AS day_index
FROM _new_diaries nd;

-- 5) diary_emotions: 메인 태그 1개 + 서브 태그 1개 + 5일마다 중립 태그 1개
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
    confidence,
    created_at,
    updated_at
)
SELECT
    b.diary_id,
    'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid,
    b.diary_date,
    t.id,
    t.category,
    t.name,
    t.color,
    CASE
        WHEN t.category = 'NEGATIVE' THEN 4
        WHEN t.category = 'POSITIVE' THEN 3
        ELSE 2
    END,
    'MANUAL'::public.emotion_source,
    NULL,
    now(),
    now()
FROM _new_diary_base b
JOIN public.emotion_tags t
  ON t.profile_id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
 AND t.name = CASE (b.day_index % 10)
     WHEN 0 THEN '행복'
     WHEN 1 THEN '감사'
     WHEN 2 THEN '평온'
     WHEN 3 THEN '뿌듯'
     WHEN 4 THEN '불안'
     WHEN 5 THEN '초조'
     WHEN 6 THEN '지침'
     WHEN 7 THEN '답답'
     WHEN 8 THEN '슬픔'
     ELSE '희망'
 END
ON CONFLICT (diary_id, emotion_tag_id, source) DO NOTHING;

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
    confidence,
    created_at,
    updated_at
)
SELECT
    b.diary_id,
    'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid,
    b.diary_date,
    t.id,
    t.category,
    t.name,
    t.color,
    CASE
        WHEN t.category = 'NEGATIVE' THEN 3
        WHEN t.category = 'POSITIVE' THEN 2
        ELSE 2
    END,
    'MANUAL'::public.emotion_source,
    NULL,
    now(),
    now()
FROM _new_diary_base b
JOIN public.emotion_tags t
  ON t.profile_id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
 AND t.name = CASE (b.day_index % 6)
     WHEN 0 THEN '집중'
     WHEN 1 THEN '무난'
     WHEN 2 THEN '불안'
     WHEN 3 THEN '감사'
     WHEN 4 THEN '답답'
     ELSE '평온'
 END
ON CONFLICT (diary_id, emotion_tag_id, source) DO NOTHING;

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
    confidence,
    created_at,
    updated_at
)
SELECT
    b.diary_id,
    'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid,
    b.diary_date,
    t.id,
    t.category,
    t.name,
    t.color,
    2,
    'MANUAL'::public.emotion_source,
    NULL,
    now(),
    now()
FROM _new_diary_base b
JOIN public.emotion_tags t
  ON t.profile_id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
 AND t.name = '무난'
WHERE (b.day_index % 5) = 0
ON CONFLICT (diary_id, emotion_tag_id, source) DO NOTHING;

-- 6) diary_tags 동기화 (manual source 기준)
INSERT INTO public.diary_tags (diary_id, emotion_tag_id, created_at, updated_at)
SELECT
    de.diary_id,
    de.emotion_tag_id,
    now(),
    now()
FROM public.diary_emotions de
JOIN _new_diaries nd ON nd.id = de.diary_id
WHERE de.source = 'MANUAL'
ON CONFLICT (diary_id, emotion_tag_id) DO NOTHING;

-- 7) usage_count 재계산 (대상 프로필 커스텀 태그만)
UPDATE public.emotion_tags et
SET usage_count = calc.cnt,
    updated_at = now()
FROM (
    SELECT
        t.id AS tag_id,
        COALESCE(COUNT(dt.id), 0)::int AS cnt
    FROM public.emotion_tags t
    LEFT JOIN public.diary_tags dt
      ON dt.emotion_tag_id = t.id
    LEFT JOIN public.diaries d
      ON d.id = dt.diary_id
     AND d.is_deleted = false
    WHERE t.profile_id = 'eb2d1a56-1f10-4d11-8477-db397929b9a1'::uuid
    GROUP BY t.id
) calc
WHERE et.id = calc.tag_id;

-- 8) 결과 요약 출력
DO $$
DECLARE
    v_diary_count int;
    v_diary_emotion_count int;
    v_diary_tag_count int;
BEGIN
    SELECT COUNT(*) INTO v_diary_count FROM _new_diaries;
    SELECT COUNT(*) INTO v_diary_emotion_count
    FROM public.diary_emotions de
    JOIN _new_diaries nd ON nd.id = de.diary_id;
    SELECT COUNT(*) INTO v_diary_tag_count
    FROM public.diary_tags dt
    JOIN _new_diaries nd ON nd.id = dt.diary_id;

    RAISE NOTICE 'seed completed: new diaries=%, diary_emotions=%, diary_tags=%',
        v_diary_count, v_diary_emotion_count, v_diary_tag_count;
END$$;

COMMIT;
