-- ============================================
-- Mindlog Database Schema
-- Generated from JPA Entities
-- ============================================

-- Enum Types
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE auth_provider AS ENUM ('GOOGLE', 'KAKAO', 'APPLE', 'EMAIL');
CREATE TYPE emotion_category AS ENUM ('POSITIVE', 'NEGATIVE', 'NEUTRAL');

-- ============================================
-- Table: profiles
-- ============================================
CREATE TABLE profiles (
    id UUID NOT NULL,
    avatar TEXT,
    name TEXT NOT NULL,
    user_name TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    role user_role NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT profiles_pkey PRIMARY KEY (id)
);

-- ============================================
-- Table: users
-- ============================================
CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR,
    name VARCHAR,
    provider auth_provider NOT NULL,
    provider_id VARCHAR NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

-- ============================================
-- Table: diaries
-- ============================================
CREATE TABLE diaries (
    id BIGSERIAL NOT NULL,
    profile_id UUID NOT NULL,
    date DATE NOT NULL,
    short_content TEXT,
    situation TEXT,
    reaction TEXT,
    physical_sensation TEXT,
    desired_reaction TEXT,
    gratitude_moment TEXT,
    self_kind_words TEXT,
    image_url TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT diaries_pkey PRIMARY KEY (id),
    CONSTRAINT diaries_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profiles(id)
);

-- Index for soft delete filtering
CREATE INDEX idx_diaries_is_deleted ON diaries(is_deleted) WHERE is_deleted = false;
CREATE INDEX idx_diaries_profile_id ON diaries(profile_id);
CREATE INDEX idx_diaries_date ON diaries(date);

-- ============================================
-- Table: emotion_tags
-- ============================================
CREATE TABLE emotion_tags (
    id BIGSERIAL NOT NULL,
    profile_id UUID,
    name TEXT NOT NULL,
    color TEXT DEFAULT '#6B7280',
    category emotion_category,
    is_default BOOLEAN NOT NULL DEFAULT false,
    usage_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT emotion_tags_pkey PRIMARY KEY (id),
    CONSTRAINT emotion_tags_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profiles(id),
    CONSTRAINT emotion_tags_profile_id_name_unique UNIQUE (profile_id, name)
);

-- Index for default tags and profile tags
CREATE INDEX idx_emotion_tags_profile_id ON emotion_tags(profile_id);
CREATE INDEX idx_emotion_tags_is_default ON emotion_tags(is_default);
CREATE INDEX idx_emotion_tags_usage_count ON emotion_tags(usage_count DESC);

-- Partial unique index for default tags (profile_id IS NULL)
-- PostgreSQL treats NULL as distinct values in UNIQUE constraints,
-- so we need a partial index to prevent duplicate default tag names.
CREATE UNIQUE INDEX idx_unique_default_tag_name ON emotion_tags(name) WHERE profile_id IS NULL;

-- ============================================
-- Table: diary_tags
-- ============================================
CREATE TABLE diary_tags (
    id BIGSERIAL NOT NULL,
    diary_id BIGINT NOT NULL,
    emotion_tag_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT diary_tags_pkey PRIMARY KEY (id),
    CONSTRAINT diary_tags_diary_id_fkey FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE,
    CONSTRAINT diary_tags_emotion_tag_id_fkey FOREIGN KEY (emotion_tag_id) REFERENCES emotion_tags(id) ON DELETE CASCADE
);

-- Index for diary tags lookup
CREATE INDEX idx_diary_tags_diary_id ON diary_tags(diary_id);
CREATE INDEX idx_diary_tags_emotion_tag_id ON diary_tags(emotion_tag_id);
CREATE UNIQUE INDEX idx_diary_tags_diary_emotion_unique ON diary_tags(diary_id, emotion_tag_id);

-- ============================================
-- Table: notification_settings
-- ============================================
CREATE TABLE notification_settings (
    id BIGSERIAL NOT NULL,
    profile_id UUID NOT NULL UNIQUE,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    reminder_times TIME[],
    custom_message TEXT,
    push_subscription JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_settings_pkey PRIMARY KEY (id),
    CONSTRAINT notification_settings_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profiles(id)
);

-- Index for enabled notifications
CREATE INDEX idx_notification_settings_is_enabled ON notification_settings(is_enabled) WHERE is_enabled = true;

-- ============================================
-- Comments
-- ============================================
COMMENT ON TABLE profiles IS '사용자 프로필 정보';
COMMENT ON TABLE users IS 'OAuth 인증 사용자 정보';
COMMENT ON TABLE diaries IS '감정 일기';
COMMENT ON TABLE emotion_tags IS '감정 태그 (기본 태그와 커스텀 태그)';
COMMENT ON TABLE diary_tags IS '일기와 감정 태그의 연결 테이블';
COMMENT ON TABLE notification_settings IS '알림 설정';

COMMENT ON COLUMN emotion_tags.profile_id IS 'null이면 기본 태그, 값이 있으면 해당 프로필의 커스텀 태그';
COMMENT ON COLUMN emotion_tags.is_default IS '기본 태그 여부';
COMMENT ON COLUMN diaries.is_deleted IS '소프트 삭제 플래그';
