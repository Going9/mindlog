import { forwardRef, useRef, useImperativeHandle } from "react";
import { DiaryCalendar } from "./diary-calendar";
import { ConditionalDiaryButton, type ConditionalDiaryButtonRef } from "./conditional-diary-button";

type DiarySidebarProps = {
  calendarDates: string[]; // 날짜 문자열 배열
  selectedDate?: Date;
  onDateSelect: (date: Date | undefined) => void;
  profileId: string;
};

export interface DiarySidebarRef {
  refreshDiaryButton: () => void;
}

export const DiarySidebar = forwardRef<DiarySidebarRef, DiarySidebarProps>(({
  calendarDates,
  selectedDate,
  onDateSelect,
  profileId,
}, ref) => {
  const diaryButtonRef = useRef<ConditionalDiaryButtonRef>(null);

  useImperativeHandle(ref, () => ({
    refreshDiaryButton: () => {
      diaryButtonRef.current?.refresh();
    }
  }), []);

  return (
    <div className="lg:col-span-1">
      <div className="sticky top-8 space-y-4">
        <DiaryCalendar
          diaryDates={calendarDates}
          selectedDate={selectedDate}
          onDateSelect={onDateSelect}
        />
        {/* Mobile Conditional Diary Button */}
        <ConditionalDiaryButton
          ref={diaryButtonRef}
          profileId={profileId}
          className="w-full sm:hidden"
          size="lg"
        />
      </div>
    </div>
  );
});