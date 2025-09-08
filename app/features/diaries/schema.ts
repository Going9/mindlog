// src/app/features/diaries/schema.ts

/**
 * ## 파일 흐름 및 역할
 *
 * 이 파일은 데이터베이스의 테이블 구조를 코드로 정의하는 "스키마(Schema)" 파일입니다.
 * Drizzle ORM 라이브러리를 사용하여 TypeScript 코드로 테이블의 모양을 정의하면,
 * Drizzle이 이 코드를 실제 데이터베이스(여기서는 PostgreSQL)가 이해할 수 있는 SQL 언어로
 * 변환하여 테이블을 생성하고 관리해줍니다.
 *
 * ## 코드 구조 및 원리
 *
 * - **ORM (Object-Relational Mapping, 객체-관계 매핑)**:
 *   ORM은 데이터베이스 테이블과 TypeScript/JavaScript의 객체(Object)를 서로 연결(매핑)해주는 기술입니다.
 *   ORM을 사용하면 SQL 쿼리문을 직접 작성하지 않고도, 마치 자바스크립트 함수를 사용하듯이
 *   데이터를 추가(insert), 조회(select), 수정(update), 삭제(delete)할 수 있습니다.
 *   이를 통해 개발자는 데이터베이스 종류에 덜 종속되고, 더 안전하고 직관적으로 데이터를 다룰 수 있습니다.
 *
 * - `pgTable`: PostgreSQL 데이터베이스의 테이블 하나를 정의하는 함수입니다.
 *   - 첫 번째 인자: 테이블의 이름 (예: "diaries")
 *   - 두 번째 인자: 테이블의 컬럼(열)들을 정의하는 객체
 * - **컬럼 정의**: `text`, `bigint`, `timestamp` 등은 각 컬럼의 데이터 타입을 정의합니다.
 *   - `id: bigint("id", ...).primaryKey()`: `id`라는 숫자형 컬럼을 만들고, 이 테이블의 고유 식별자(Primary Key)로 지정합니다.
 *   - `profileId: uuid("profile_id").references(() => profiles.id)`: `profiles` 테이블의 `id`를 참조하는 외래 키(Foreign Key)를 설정합니다. 이를 통해 일기와 사용자 프로필을 연결합니다.
 *   - `notNull()`: 이 컬럼은 비어 있을 수 없다는 제약 조건입니다.
 *   - `defaultNow()`: 데이터가 생성될 때 현재 시간이 자동으로 기록됩니다.
 * - `unique()`: 특정 컬럼들의 조합이 테이블 내에서 항상 고유한 값을 가져야 한다는 제약 조건입니다.
 *   예를 들어, `diaries` 테이블에서는 한 명의 사용자가 같은 날짜에 두 개의 일기를 작성할 수 없습니다.
 */

import {
  pgTable, // PostgreSQL 테이블을 정의하는 함수
  bigint,    // 큰 정수 타입 (예: 1, 2, 3, ...)
  uuid,      // 고유 식별자 타입 (예: "a1b2c3d4-...")
  text,      // 문자열 타입
  timestamp, // 날짜와 시간 타입
  boolean,   // 참/거짓(true/false) 타입
  date,      // 날짜 타입
  unique,    // 고유 제약조건을 설정하는 함수
  index,     // 인덱스를 설정하는 함수
  uniqueIndex, // 고유 인덱스를 설정하는 함수
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";
import { profiles } from "../users/schema"; // users 기능 폴더에 정의된 profiles 테이블 스키마
import { emotionTags } from "../emotions/schema"; // emotions 기능 폴더에 정의된 emotionTags 테이블 스키마

// 'diaries' 테이블 정의
export const diaries = pgTable(
  "diaries", // 테이블 이름
  {
    // 컬럼(열) 정의
    id: bigint("id", { mode: "number" })
      .primaryKey() // 이 컬럼을 기본 키로 설정
      .generatedAlwaysAsIdentity(), // 데이터베이스가 자동으로 1씩 증가하는 고유 번호를 생성

    profileId: uuid("profile_id") // 작성자의 프로필 ID
      .notNull() // 비어있을 수 없음
      .references(() => profiles.id, { onDelete: "cascade" }), // profiles 테이블의 id를 참조. 프로필이 삭제되면 관련 일기도 함께 삭제됨 (onDelete: "cascade")

    date: date("date").notNull(), // 일기 작성 날짜
    shortContent: text("short_content"), // 한 줄 요약
    situation: text("situation"), // 상황
    reaction: text("reaction"), // 감정적/생각적 반응
    physicalSensation: text("physical_sensation"), // 신체적 감각
    desiredReaction: text("desired_reaction"), // 원했던 반응
    gratitudeMoment: text("gratitude_moment"), // 감사한 순간
    selfKindWords: text("self_kind_words"), // 자신에게 하고 싶은 말
    imageUrl: text("image_url"), // 첨부 이미지의 URL
    isDeleted: boolean("is_deleted").default(false).notNull(), // 삭제 여부 (소프트 삭제를 위함)
    deletedAt: timestamp("deleted_at", { withTimezone: true }), // 삭제된 시간 (삭제 시에만 설정)

    createdAt: timestamp("created_at", { withTimezone: true })
      .defaultNow() // 생성 시 자동으로 현재 시간 저장
      .notNull(),

    updatedAt: timestamp("updated_at", { withTimezone: true })
      .defaultNow() // 수정 시 자동으로 현재 시간 저장
      .notNull(),
  },
  // 테이블 레벨 제약 조건 정의
  table => ({
    // 활성 일기(is_deleted = false)에 대해서만 고유성 보장하는 부분 인덱스
    // 삭제된 일기들은 제약 조건에서 제외되어 여러 개 존재할 수 있음
    uniqueProfileDateActive: uniqueIndex("unique_profile_date_active")
      .on(table.profileId, table.date)
      .where(sql`${table.isDeleted} = false`),
  })
);

// 'diary_tags' 테이블 정의 (다대다 관계를 위한 중간 테이블)
// 하나의 일기(diary)는 여러 개의 감정 태그(emotionTag)를 가질 수 있고,
// 하나의 감정 태그는 여러 개의 일기에 사용될 수 있습니다.
// 이 관계를 표현하기 위해 중간에서 두 테이블을 연결해주는 테이블입니다.
export const diaryTags = pgTable(
  "diary_tags", // 테이블 이름
  {
    id: bigint("id", { mode: "number" }).primaryKey().generatedAlwaysAsIdentity(),

    diaryId: bigint("diary_id", { mode: "number" })
      .notNull()
      .references(() => diaries.id, { onDelete: "cascade" }), // diaries 테이블의 id 참조

    emotionTagId: bigint("emotion_tag_id", { mode: "number" })
      .notNull()
      .references(() => emotionTags.id, { onDelete: "cascade" }), // emotionTags 테이블의 id 참조

    createdAt: timestamp("created_at", { withTimezone: true })
      .defaultNow()
      .notNull(),
  },
  // 테이블 레벨 제약 조건 정의
  table => ({
    // 하나의 일기에 동일한 감정 태그가 중복으로 연결되는 것을 방지
    uniqueDiaryTag: unique().on(table.diaryId, table.emotionTagId),
  })
);
