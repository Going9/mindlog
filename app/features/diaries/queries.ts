import client from "~/supa-client";

type GetDiariesOptions = {
  profileId: string;
  limit?: number;
  offset?: number;
  sortBy?: "date-asc" | "date-desc" | "completion-asc" | "completion-desc";
  searchQuery?: string;
  date?: Date;
  emotionTagId?: number;
};

export const getDiaries = async ({
  profileId,
  limit = 20,
  offset = 0,
  sortBy = "date-desc",
  searchQuery,
  date,
  emotionTagId,
}: GetDiariesOptions) => {
  // 감정 태그 필터가 있는 경우와 없는 경우를 분리하여 처리
  if (emotionTagId) {
    // 감정 태그 필터가 있는 경우: JOIN을 사용하여 정확한 필터링
    let emotionQuery = client
      .from("diaries")
      .select(
        `
        id,
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
        updated_at,
        diary_tags!inner(emotion_tag_id)
      `
      )
      .eq("profile_id", profileId)
      .eq("is_deleted", false)
      .eq("diary_tags.emotion_tag_id", emotionTagId);

    // Add search condition
    if (searchQuery) {
      emotionQuery = emotionQuery.or(
        `short_content.ilike.%${searchQuery}%,situation.ilike.%${searchQuery}%,reaction.ilike.%${searchQuery}%`
      );
    }

    // Add date condition
    if (date) {
      emotionQuery = emotionQuery.eq("date", date.toISOString().split("T")[0]);
    }

    const { data: diariesData, error: diariesError } = await emotionQuery
      .order("date", { ascending: sortBy === "date-asc" })
      .range(offset, offset + limit - 1);

    if (diariesError) throw diariesError;

    // 각 일기의 모든 감정 태그를 별도로 가져옴
    const diaryIds = diariesData.map(d => d.id);
    let allTags: any[] = [];

    if (diaryIds.length > 0) {
      const { data: tagsData, error: tagsError } = await client
        .from("diary_tags")
        .select(
          `
          diary_id,
          emotion_tags (
            id,
            name,
            color,
            category,
            is_default
          )
        `
        )
        .in("diary_id", diaryIds);

      if (tagsError) throw tagsError;

      allTags = tagsData.map(tagRelation => ({
        diary_id: tagRelation.diary_id,
        id: (tagRelation.emotion_tags as any).id,
        name: (tagRelation.emotion_tags as any).name,
        color: (tagRelation.emotion_tags as any).color,
        category: (tagRelation.emotion_tags as any).category,
        is_default: (tagRelation.emotion_tags as any).is_default,
      }));
    }

    // 태그를 다이어리별로 그룹화
    const tagsByDiaryId = allTags.reduce(
      (acc, tag) => {
        if (!acc[tag.diary_id]) {
          acc[tag.diary_id] = [];
        }
        acc[tag.diary_id].push({
          id: tag.id,
          name: tag.name,
          color: tag.color,
          category: tag.category,
          isDefault: tag.is_default,
        });
        return acc;
      },
      {} as Record<number, any[]>
    );

    return diariesData.map((diary: any) => ({
      id: diary.id,
      profileId: diary.profile_id,
      date: diary.date,
      shortContent: diary.short_content,
      situation: diary.situation,
      reaction: diary.reaction,
      physicalSensation: diary.physical_sensation,
      desiredReaction: diary.desired_reaction,
      gratitudeMoment: diary.gratitude_moment,
      selfKindWords: diary.self_kind_words,
      imageUrl: diary.image_url,
      isDeleted: diary.is_deleted,
      createdAt: diary.created_at,
      updatedAt: diary.updated_at,
      emotionTags: tagsByDiaryId[diary.id] || [],
      completedSteps: calculateCompletedSteps(diary),
      totalSteps: 7,
    }));
  } else {
    // 감정 태그 필터가 없는 경우: 뷰를 사용
    let query = (client as any)
      .from("diary_with_emotion_tags")
      .select("*")
      .eq("profile_id", profileId);

    // Add search condition
    if (searchQuery) {
      query = query.or(
        `short_content.ilike.%${searchQuery}%,situation.ilike.%${searchQuery}%,reaction.ilike.%${searchQuery}%`
      );
    }

    // Add date condition
    if (date) {
      query = query.eq("date", date.toISOString().split("T")[0]);
    }

    // Apply ordering
    if (sortBy === "date-asc" || sortBy === "date-desc") {
      query = query.order("date", { ascending: sortBy === "date-asc" });
    } else {
      query = query.order("date", { ascending: false });
    }

    const { data, error } = await query.range(offset, offset + limit - 1);

    if (error) throw error;

    // Map the view data to match the expected format
    let diariesWithTags = data.map((diary: any) => ({
      id: diary.id,
      profileId: diary.profile_id,
      date: diary.date,
      shortContent: diary.short_content,
      situation: diary.situation,
      reaction: diary.reaction,
      physicalSensation: diary.physical_sensation,
      desiredReaction: diary.desired_reaction,
      gratitudeMoment: diary.gratitude_moment,
      selfKindWords: diary.self_kind_words,
      imageUrl: diary.image_url,
      isDeleted: diary.is_deleted,
      createdAt: diary.created_at,
      updatedAt: diary.updated_at,
      emotionTags: diary.emotion_tags || [],
      completedSteps: diary.completed_steps,
      totalSteps: diary.total_steps,
    }));

    // Apply completion sorting if needed (post-processing)
    if (sortBy === "completion-asc" || sortBy === "completion-desc") {
      diariesWithTags.sort((a: any, b: any) => {
        const aCompletion = a.completedSteps / a.totalSteps;
        const bCompletion = b.completedSteps / b.totalSteps;
        return sortBy === "completion-asc"
          ? aCompletion - bCompletion
          : bCompletion - aCompletion;
      });
    }

    return diariesWithTags;
  }
};

// 캘린더용 경량 함수 - 날짜만 가져옴
export const getDiaryDatesForCalendar = async (
  profileId: string,
  year?: number
): Promise<string[]> => {
  let query = client
    .from("diaries")
    .select("date")
    .eq("profile_id", profileId)
    .eq("is_deleted", false);

  // 년도 필터링
  if (year) {
    const startDate = `${year}-01-01`;
    const endDate = `${year}-12-31`;
    query = query.gte("date", startDate).lte("date", endDate);
  }

  const { data, error } = await query;

  if (error) throw error;

  // 중복 제거하고 날짜 문자열만 반환
  const uniqueDates = [...new Set(data.map(item => item.date))];
  return uniqueDates.sort();
};

// 단일 일기 조회
export const getDiaryById = async (diaryId: number, profileId: string) => {
  // 일기 기본 정보 조회
  const { data: diaryData, error: diaryError } = await client
    .from("diaries")
    .select("*")
    .eq("id", diaryId)
    .eq("profile_id", profileId)
    .eq("is_deleted", false)
    .single();

  if (diaryError) throw diaryError;
  if (!diaryData) return null;

  // 일기의 감정 태그들 조회
  const { data: tagsData, error: tagsError } = await client
    .from("diary_tags")
    .select(
      `
      emotion_tags (
        id,
        name,
        color,
        category,
        is_default
      )
    `
    )
    .eq("diary_id", diaryId);

  if (tagsError) throw tagsError;

  const emotionTags = tagsData.map((tagRelation: any) => ({
    id: tagRelation.emotion_tags.id,
    name: tagRelation.emotion_tags.name,
    color: tagRelation.emotion_tags.color,
    category: tagRelation.emotion_tags.category,
    isDefault: tagRelation.emotion_tags.is_default,
  }));

  return {
    id: diaryData.id,
    profileId: diaryData.profile_id,
    date: new Date(diaryData.date),
    shortContent: diaryData.short_content,
    situation: diaryData.situation,
    reaction: diaryData.reaction,
    physicalSensation: diaryData.physical_sensation,
    desiredReaction: diaryData.desired_reaction,
    gratitudeMoment: diaryData.gratitude_moment,
    selfKindWords: diaryData.self_kind_words,
    imageUrl: diaryData.image_url,
    isDeleted: diaryData.is_deleted,
    createdAt: diaryData.created_at,
    updatedAt: diaryData.updated_at,
    emotionTags,
    completedSteps: calculateCompletedSteps(diaryData),
    totalSteps: 7,
  };
};

// 완료 단계 계산 함수
function calculateCompletedSteps(diary: any): number {
  const fields = [
    diary.short_content,
    diary.situation,
    diary.reaction,
    diary.physical_sensation,
    diary.desired_reaction,
    diary.gratitude_moment,
    diary.self_kind_words,
  ];

  return fields.filter(
    field =>
      field !== null &&
      field !== undefined &&
      field !== "" &&
      field.trim() !== ""
  ).length;
}
