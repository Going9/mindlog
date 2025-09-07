import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { SteppedDiaryForm } from "../components/stepped-diary-form";
import { getDiaries, updateDiary } from "../queries";
import { getEmotionTags } from "../../emotions/queries";
import type { Route } from "./+types/edit-diary";

// --- 타입 정의 ---
interface EmotionTag {
  id: number;
  name: string;
  color: string | null;
  category: "positive" | "negative" | "neutral" | null;
  isDefault: boolean | null;
  usageCount?: number | null;
}

interface DiaryFormData {
  date: Date;
  shortContent?: string;
  situation?: string;
  reaction?: string;
  physicalSensation?: string;
  desiredReaction?: string;
  gratitudeMoment?: string;
  selfKindWords?: string;
  imageFile?: File;
  emotionTags?: EmotionTag[];
}

export const loader = async ({ params }: { params: { date: string } }) => {
  const profileId = "b0e0e902-3488-4c10-9621-fffde048923c";
  const date = params.date;
  
  if (!date) {
    throw new Response("Date parameter is required", { status: 400 });
  }

  // 날짜로 일기 찾기
  const targetDate = new Date(date);
  
  // 해당 날짜의 모든 일기를 조회해서 일치하는 것 찾기
  const diaries = await getDiaries({
    profileId,
    date: targetDate,
    limit: 1,
    offset: 0,
  });

  if (!diaries || diaries.length === 0) {
    throw new Response("Diary not found", { status: 404 });
  }

  const diary = diaries[0];
  
  // 감정 태그도 로드
  const emotionTags = await getEmotionTags(profileId);
  
  return {
    diary,
    emotionTags,
  };
};

// 일기 수정 페이지 컴포넌트
export default function EditDiaryPage({ loaderData }: Route["ComponentProps"]) {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const { diary, emotionTags } = loaderData;
  
  const profileId = "b0e0e902-3488-4c10-9621-fffde048923c";

  // 초기 데이터 변환
  const initialData: Partial<DiaryFormData> = {
    date: new Date(diary.date),
    shortContent: diary.shortContent || "",
    situation: diary.situation || "",
    reaction: diary.reaction || "",
    physicalSensation: diary.physicalSensation || "",
    desiredReaction: diary.desiredReaction || "",
    gratitudeMoment: diary.gratitudeMoment || "",
    selfKindWords: diary.selfKindWords || "",
    emotionTags: diary.emotionTags || [],
  };

  // 폼이 최종적으로 제출될 때 실행될 함수
  const handleSubmit = async (data: DiaryFormData) => {
    setIsLoading(true);

    try {
      const updatedDiary = await updateDiary(diary.id, profileId, {
        shortContent: data.shortContent,
        situation: data.situation,
        reaction: data.reaction,
        physicalSensation: data.physicalSensation,
        desiredReaction: data.desiredReaction,
        gratitudeMoment: data.gratitudeMoment,
        selfKindWords: data.selfKindWords,
        imageFile: data.imageFile,
        emotionTags: data.emotionTags,
      });

      console.log("일기 수정 완료:", updatedDiary);
      alert("일기가 성공적으로 수정되었습니다!");
      navigate("/diary");
    } catch (error) {
      console.error("일기 수정 중 에러 발생:", error);
      alert("일기 수정 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsLoading(false);
    }
  };

  // 각 단계를 저장할 때 실행될 함수
  const handleSaveStep = async (data: Partial<DiaryFormData>, step: number) => {
    try {
      console.log(`${step} 단계 중간 저장:`, data);
      // 중간 저장 로직 (필요시 구현)
      await new Promise(resolve => setTimeout(resolve, 500));
    } catch (error) {
      console.error("단계 저장 중 에러 발생:", error);
    }
  };

  return (
    <SteppedDiaryForm
      initialData={initialData}
      onSubmit={handleSubmit}
      onSave={handleSaveStep}
      isLoading={isLoading}
      isEditing={true}
      availableEmotionTags={emotionTags}
    />
  );
}