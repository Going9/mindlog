import db from "~/db";
import { emotionTags } from "./schema";
import { eq, or, isNull } from "drizzle-orm";

export const getEmotionTags = async (profileId: string) => {
  const tags = await db
    .select({
      id: emotionTags.id,
      name: emotionTags.name,
      color: emotionTags.color,
      category: emotionTags.category,
      isDefault: emotionTags.isDefault,
      usageCount: emotionTags.usageCount,
    })
    .from(emotionTags)
    .where(
      or(eq(emotionTags.profileId, profileId), eq(emotionTags.isDefault, true))
    );

  return tags;
};

export const getDefaultEmotionTags = async () => {
  const defaultTags = await db
    .select({
      id: emotionTags.id,
      name: emotionTags.name,
      color: emotionTags.color,
      category: emotionTags.category,
      isDefault: emotionTags.isDefault,
      usageCount: emotionTags.usageCount,
    })
    .from(emotionTags)
    .where(eq(emotionTags.isDefault, true));

  return defaultTags;
};

export const getUserCustomEmotionTags = async (profileId: string) => {
  const customTags = await db
    .select({
      id: emotionTags.id,
      name: emotionTags.name,
      color: emotionTags.color,
      category: emotionTags.category,
      isDefault: emotionTags.isDefault,
      usageCount: emotionTags.usageCount,
    })
    .from(emotionTags)
    .where(eq(emotionTags.profileId, profileId));

  return customTags;
};

// 중복 태그 이름 체크 함수
export const checkDuplicateTagName = async (
  profileId: string,
  name: string
) => {
  const trimmedName = name.trim().toLowerCase();

  // 사용자의 커스텀 태그 중에 중복이 있는지 확인
  const existingCustomTag = await db
    .select({ id: emotionTags.id })
    .from(emotionTags)
    .where(eq(emotionTags.profileId, profileId))
    .limit(1);

  if (existingCustomTag.length > 0) {
    // 실제 이름 비교는 클라이언트에서 처리하거나, SQL에서 LOWER 함수 사용 필요
    const userTags = await db
      .select({
        name: emotionTags.name,
      })
      .from(emotionTags)
      .where(eq(emotionTags.profileId, profileId));

    const isDuplicate = userTags.some(
      tag => tag.name.toLowerCase() === trimmedName
    );
    if (isDuplicate) return true;
  }

  // 기본 태그 중에 중복이 있는지 확인
  const defaultTags = await db
    .select({
      name: emotionTags.name,
    })
    .from(emotionTags)
    .where(eq(emotionTags.isDefault, true));

  const isDefaultDuplicate = defaultTags.some(
    tag => tag.name.toLowerCase() === trimmedName
  );

  return isDefaultDuplicate;
};

// 커스텀 감정 태그 생성 함수
export const createCustomEmotionTag = async (
  profileId: string,
  name: string,
  color: string,
  category: "positive" | "negative" | "neutral"
) => {
  const trimmedName = name.trim();

  // 빈 이름 체크
  if (!trimmedName) {
    throw new Error("태그 이름을 입력해주세요.");
  }

  // 중복 체크
  const isDuplicate = await checkDuplicateTagName(profileId, trimmedName);
  if (isDuplicate) {
    throw new Error("이미 존재하는 태그 이름입니다.");
  }

  // 새 커스텀 태그 생성
  const [newTag] = await db
    .insert(emotionTags)
    .values({
      profileId,
      name: trimmedName,
      color,
      category,
      isDefault: false,
      usageCount: 0,
    })
    .returning({
      id: emotionTags.id,
      name: emotionTags.name,
      color: emotionTags.color,
      category: emotionTags.category,
      isDefault: emotionTags.isDefault,
      usageCount: emotionTags.usageCount,
    });

  return newTag;
};

// 커스텀 감정 태그 삭제 함수
export const deleteCustomEmotionTag = async (
  profileId: string,
  tagId: number
) => {
  // 먼저 해당 태그가 사용자의 커스텀 태그인지 확인
  const [existingTag] = await db
    .select({
      id: emotionTags.id,
      isDefault: emotionTags.isDefault,
      profileId: emotionTags.profileId,
    })
    .from(emotionTags)
    .where(eq(emotionTags.id, tagId))
    .limit(1);

  if (!existingTag) {
    throw new Error("존재하지 않는 태그입니다.");
  }

  if (existingTag.isDefault) {
    throw new Error("기본 태그는 삭제할 수 없습니다.");
  }

  if (existingTag.profileId !== profileId) {
    throw new Error("다른 사용자의 태그는 삭제할 수 없습니다.");
  }

  // 태그 삭제
  const [deletedTag] = await db
    .delete(emotionTags)
    .where(eq(emotionTags.id, tagId))
    .returning({
      id: emotionTags.id,
      name: emotionTags.name,
    });

  return deletedTag;
};
