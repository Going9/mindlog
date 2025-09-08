import { forwardRef, useRef, useImperativeHandle } from "react";
import { ConditionalDiaryButton, type ConditionalDiaryButtonRef } from "./conditional-diary-button";

type DiaryListHeaderProps = {
  totalDiaries: number;
  profileId: string;
};

export interface DiaryListHeaderRef {
  refreshDiaryButton: () => void;
}

export const DiaryListHeader = forwardRef<DiaryListHeaderRef, DiaryListHeaderProps>(({ totalDiaries, profileId }, ref) => {
  const diaryButtonRef = useRef<ConditionalDiaryButtonRef>(null);

  useImperativeHandle(ref, () => ({
    refreshDiaryButton: () => {
      diaryButtonRef.current?.refresh();
    }
  }), []);

  return (
    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">일기 목록</h1>
        <p className="text-muted-foreground mt-2">
          {totalDiaries}개의 일기가 있습니다
        </p>
      </div>
      {/* Desktop Conditional Diary Button */}
      <ConditionalDiaryButton
        ref={diaryButtonRef}
        profileId={profileId}
        className="hidden sm:flex"
        size="lg"
      />
    </div>
  );
});