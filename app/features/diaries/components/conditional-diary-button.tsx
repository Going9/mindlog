import { Link } from "react-router";
import { useEffect, useState, useCallback, forwardRef, useImperativeHandle } from "react";
import { Button } from "~/common/components/ui/button";
import { LoadingSpinner } from "~/common/components/ui/loading";
import { PlusIcon, EditIcon } from "lucide-react";
import { getTodayDiary } from "../queries";

type ConditionalDiaryButtonProps = {
  profileId: string;
  className?: string;
  size?: "default" | "sm" | "lg" | "icon" | null | undefined;
};

export interface ConditionalDiaryButtonRef {
  refresh: () => void;
}

export const ConditionalDiaryButton = forwardRef<ConditionalDiaryButtonRef, ConditionalDiaryButtonProps>(({ 
  profileId, 
  className = "", 
  size = "lg" 
}, ref) => {
  const [todayDiaryId, setTodayDiaryId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const checkTodayDiary = useCallback(async () => {
    try {
      setIsLoading(true);
      const diaryId = await getTodayDiary(profileId);
      setTodayDiaryId(diaryId);
    } catch (error) {
      console.error("오늘 일기 확인 중 오류:", error);
      setTodayDiaryId(null);
    } finally {
      setIsLoading(false);
    }
  }, [profileId]);

  useEffect(() => {
    checkTodayDiary();
  }, [checkTodayDiary]);

  useImperativeHandle(ref, () => ({
    refresh: checkTodayDiary
  }), [checkTodayDiary]);

  if (isLoading) {
    return (
      <Button disabled size={size} className={className}>
        <LoadingSpinner size="sm" text="확인 중..." />
      </Button>
    );
  }

  if (todayDiaryId) {
    // 오늘 일기가 있으면 수정하기 버튼
    return (
      <Button asChild size={size} className={className}>
        <Link to={`/diary/${todayDiaryId}/edit`}>
          <EditIcon className="w-4 h-4 mr-2" />
          오늘 일기 수정하기
        </Link>
      </Button>
    );
  }

  // 오늘 일기가 없으면 새로 쓰기 버튼
  return (
    <Button asChild size={size} className={className}>
      <Link to="/diary/new">
        <PlusIcon className="w-4 h-4 mr-2" />
        새 일기 쓰기
      </Link>
    </Button>
  );
});