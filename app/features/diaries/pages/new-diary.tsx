// src/app/features/diaries/pages/new-diary.tsx

/**
 * ## 페이지 흐름 및 역할
 *
 * 이 페이지는 사용자가 새로운 일기를 작성하는 페이지입니다.
 * 실제 UI는 `SteppedDiaryForm` 컴포넌트가 대부분을 담당하고, 이 파일은 그 컴포넌트를
 * 페이지로 "호스팅"하면서, 폼에서 발생하는 핵심적인 이벤트(최종 제출, 중간 저장)를
 * 실제로 처리하는 로직을 담고 있습니다.
 *
 * ## 코드 구조 및 원리
 *
 * - **컨테이너/프레젠테이션 패턴 (Container/Presentational Pattern)**:
 *   이 `NewDiaryPage` 컴포넌트는 "컨테이너" 역할을 합니다. 즉, "어떻게 보일지"보다는
 *   "어떻게 작동할지"에 대한 책임을 가집니다. 데이터 처리, 서버와의 통신(API 호출),
 *   페이지 이동과 같은 로직을 여기서 처리합니다.
 *   반면, `SteppedDiaryForm`은 "프레젠테이션" 역할을 하며, 데이터를 어떻게 보여주고
 *   사용자 입력을 받을지에만 집중합니다. `SteppedDiaryForm`은 로직을 직접 처리하는 대신,
 *   이벤트가 발생하면 부모(`NewDiaryPage`)로부터 받은 함수(`handleSubmit`, `handleSave`)를
 *   호출하여 부모에게 알립니다.
 *   이 패턴은 UI와 로직을 분리하여 코드를 더 재사용하기 쉽고 테스트하기 쉽게 만듭니다.
 *
 * - `useNavigate`: React Router가 제공하는 Hook으로, 코드 내에서 특정 경로로 페이지를
 *   이동시킬 때 사용합니다. 여기서는 일기 작성이 완료된 후 목록 페이지(`/diary`)로
 *   돌아가기 위해 사용됩니다.
 * - `handleSubmit`: `SteppedDiaryForm`에서 "완료하기" 버튼을 눌렀을 때 최종적으로 호출되는 함수입니다.
 *   이 함수 안에서 서버로 데이터를 전송하는 API 호출이 이루어져야 합니다. (현재는 콘솔 로그와 임시 대기로 구현)
 */

import { useState } from "react";
import { useNavigate } from "react-router";
import { SteppedDiaryForm } from "../components/stepped-diary-form";
import { createDiary } from "../queries";
import { useAuthContext } from "~/features/auth";

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

// "새 일기 쓰기" 페이지 컴포넌트
export default function NewDiaryPage() {
  // useNavigate Hook을 호출하여 페이지 이동 함수를 가져옵니다.
  const navigate = useNavigate();
  // API 호출과 같은 비동기 작업이 진행 중인지 여부를 관리하는 상태
  const [isLoading, setIsLoading] = useState(false);
  // 인증된 사용자 정보
  const { user } = useAuthContext();

  // 폼이 최종적으로 제출될 때 실행될 함수
  const handleSubmit = async (data: DiaryFormData) => {
    setIsLoading(true); // 로딩 상태 시작

    try {
      // 실제 일기 데이터 저장
      const profileId = user?.id;
      
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

      console.log("일기 데이터 저장 중:", diaryData);
      
      const createdDiary = await createDiary(diaryData);
      console.log("일기 저장 완료:", createdDiary);

      // 저장이 성공하면, 해당 날짜로 필터링된 일기 목록 페이지로 사용자를 이동시킵니다.
      const dateString = data.date.toISOString().split('T')[0]; // YYYY-MM-DD 형식으로 변환
      navigate(`/diary?date=${dateString}&page=1`);
    } catch (error) {
      console.error("일기 저장 중 에러 발생:", error);
      // TODO: 사용자에게 에러가 발생했음을 알리는 UI 처리 (예: 토스트 메시지)
      alert("일기 저장 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsLoading(false); // 작업이 성공하든 실패하든 로딩 상태 종료
    }
  };

  // 각 단계를 저장할 때 실행될 함수 (현재는 콘솔 로그만 출력)
  const handleSaveStep = async (data: Partial<DiaryFormData>, step: number) => {
    try {
      // TODO: API를 호출하여 단계별로 데이터를 임시 저장하는 로직 구현
      console.log(`${step} 단계 중간 저장:`, data);

      // 임시로 500ms 대기
      await new Promise(resolve => setTimeout(resolve, 500));

    } catch (error) {
      console.error("단계 저장 중 에러 발생:", error);
      // TODO: 에러 처리
    }
  };

  // 화면에는 SteppedDiaryForm 컴포넌트를 렌더링합니다.
  // 로직을 담은 함수들과 상태를 props로 내려줍니다.
  return (
    <SteppedDiaryForm
      onSubmit={handleSubmit}
      onSave={handleSaveStep}
      isLoading={isLoading}
      isEditing={false} // 새 일기 작성이므로 isEditing은 false
      profileId={user?.id || ''}
    />
  );
}