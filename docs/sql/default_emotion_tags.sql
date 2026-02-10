-- 기본 감정 태그 seed (emotion_tags)
-- profile_id = NULL + is_default = true 로 저장됩니다.
-- 중복 실행해도 안전하도록 ON CONFLICT(name) WHERE profile_id IS NULL 처리.

INSERT INTO emotion_tags (profile_id, name, color, category, is_default, usage_count)
VALUES
  (NULL, '기쁨', '#F59E0B', 'POSITIVE', true, 0),
  (NULL, '설렘', '#FB7185', 'POSITIVE', true, 0),
  (NULL, '평온', '#10B981', 'POSITIVE', true, 0),
  (NULL, '감사', '#14B8A6', 'POSITIVE', true, 0),
  (NULL, '슬픔', '#3B82F6', 'NEGATIVE', true, 0),
  (NULL, '불안', '#6366F1', 'NEGATIVE', true, 0),
  (NULL, '분노', '#EF4444', 'NEGATIVE', true, 0),
  (NULL, '지침', '#78716C', 'NEGATIVE', true, 0),
  (NULL, '당황', '#8B5CF6', 'NEGATIVE', true, 0),
  (NULL, '무덤덤', '#6B7280', 'NEUTRAL', true, 0)
ON CONFLICT (name) WHERE profile_id IS NULL
DO UPDATE SET
  color = EXCLUDED.color,
  category = EXCLUDED.category,
  is_default = true,
  updated_at = CURRENT_TIMESTAMP;
