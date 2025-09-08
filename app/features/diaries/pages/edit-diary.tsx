// src/app/features/diaries/pages/edit-diary.tsx

/**
 * ## 페이지 흐름 및 역할
 *
 * 이 페이지는 사용자가 기존 일기를 수정하는 페이지입니다.
 * 실제 UI는 `SteppedDiaryForm` 컴포넌트가 대부분을 담당하고, 이 파일은 그 컴포넌트를
 * 페이지로 "호스팅"하면서, 폼에서 발생하는 핵심적인 이벤트(최종 제출, 중간 저장)를
 * 실제로 처리하는 로직을 담고 있습니다.
 *
 * ## 코드 구조 및 원리
 *
 * - **컨테이너/프레젠테이션 패턴 (Container/Presentational Pattern)**:
 *   이 `EditDiaryPage` 컴포넌트는 "컨테이너" 역할을 합니다. 즉, "어떻게 보일지"보다는
 *   "어떻게 작동할지"에 대한 책임을 가집니다. 데이터 처리, 서버와의 통신(API 호출),
 *   페이지 이동과 같은 로직을 여기서 처리합니다.
 */

import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { SteppedDiaryForm } from "../components/stepped-diary-form";
import { createDiary, getDiaryById } from "../queries";

// --- 타입 정의 ---
interface EmotionTag {
  id: number;
  name: string;
  color: string;
  category: "positive" | "negative" | "neutral";
  isDefault: boolean;
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

// "일기 수정" 페이지 컴포넌트
export default function EditDiaryPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [isLoading, setIsLoading] = useState(false);
  const [isDataLoading, setIsDataLoading] = useState(true);
  const [initialData, setInitialData] = useState<DiaryFormData | null>(null);

  // 기존 일기 데이터 로드
  useEffect(() => {
    const loadDiaryData = async () => {
      if (!id) {
        navigate("/diary");
        return;
      }

      try {
        setIsDataLoading(true);
        const profileId = "b0e0e902-3488-4c10-9621-fffde048923c"; // 현재 하드코딩된 profileId 사용
        const diaryData = await getDiaryById(parseInt(id), profileId);
        
        if (!diaryData) {
          alert("일기를 찾을 수 없습니다.");
          navigate("/diary");
          return;
        }

        // API 데이터를 폼 데이터 형식으로 변환
        setInitialData({
          date: diaryData.date,
          shortContent: diaryData.shortContent || "",
          situation: diaryData.situation || "",
          reaction: diaryData.reaction || "",
          physicalSensation: diaryData.physicalSensation || "",
          desiredReaction: diaryData.desiredReaction || "",
          gratitudeMoment: diaryData.gratitudeMoment || "",
          selfKindWords: diaryData.selfKindWords || "",
          emotionTags: diaryData.emotionTags || [],
        });
      } catch (error) {
        console.error("일기 로드 중 에러 발생:", error);
        alert("일기를 불러오는 중 오류가 발생했습니다.");
        navigate("/diary");
      } finally {
        setIsDataLoading(false);
      }
    };

    loadDiaryData();
  }, [id, navigate]);

  // 폼이 최종적으로 제출될 때 실행될 함수
  const handleSubmit = async (data: DiaryFormData) => {
    setIsLoading(true);

    try {
      const profileId = "b0e0e902-3488-4c10-9621-fffde048923c";
      
      const diaryData = {
        profileId,
        date: data.date,
        shortContent: data.shortContent,
        situation: data.situation,
        reaction: data.reaction,
        physicalSensation: data.physicalSensation,
        desiredReaction: data.desiredReaction,
        gratitudeMoment: data.gratitudeMoment,
        selfKindWords: data.selfKindWords,
        imageUrl: data.imageFile ? undefined : undefined, // 이미지 업로드는 추후 구현
        emotionTagIds: data.emotionTags?.map(tag => tag.id) || [],
      };

      console.log("일기 데이터 업데이트 중:", diaryData);
      
      const updatedDiary = await createDiary(diaryData); // createDiary는 upsert 방식으로 작동
      console.log("일기 업데이트 완료:", updatedDiary);

      // 저장이 성공하면, 일기 목록 페이지로 사용자를 이동시킵니다.
      navigate("/diary");
    } catch (error) {
      console.error("일기 업데이트 중 에러 발생:", error);
      alert("일기 저장 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsLoading(false);
    }
  };

  // 각 단계를 저장할 때 실행될 함수
  const handleSaveStep = async (data: Partial<DiaryFormData>, step: number) => {
    try {
      console.log(`${step} 단계 중간 저장:`, data);
      await new Promise(resolve => setTimeout(resolve, 500));
    } catch (error) {
      console.error("단계 저장 중 에러 발생:", error);
    }
  };

  // 데이터 로딩 중이면 로딩 표시
  if (isDataLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">일기를 불러오는 중...</div>
      </div>
    );
  }

  // 초기 데이터가 없으면 에러 상태
  if (!initialData) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg text-red-600">일기 데이터를 불러올 수 없습니다.</div>
      </div>
    );
  }

  // 화면에는 SteppedDiaryForm 컴포넌트를 렌더링합니다.
  // 로직을 담은 함수들과 상태를 props로 내려줍니다.
  const profileId = "b0e0e902-3488-4c10-9621-fffde048923c"; // 현재 하드코딩된 profileId 사용
  
  return (
    <SteppedDiaryForm
      onSubmit={handleSubmit}
      onSave={handleSaveStep}
      isLoading={isLoading}
      isEditing={true} // 일기 수정이므로 isEditing은 true
      initialData={initialData} // 기존 일기 데이터 전달
      profileId={profileId}
    />
  );
}