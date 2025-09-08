import { ConditionalDiaryButton } from "./conditional-diary-button";

type DiaryListHeaderProps = {
  totalDiaries: number;
  profileId: string;
};

export function DiaryListHeader({ totalDiaries, profileId }: DiaryListHeaderProps) {
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
        profileId={profileId}
        className="hidden sm:flex"
        size="lg"
      />
    </div>
  );
}