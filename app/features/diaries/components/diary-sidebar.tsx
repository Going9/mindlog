import { DiaryCalendar } from "./diary-calendar";
import { ConditionalDiaryButton } from "./conditional-diary-button";

type DiarySidebarProps = {
  calendarDates: string[]; // 날짜 문자열 배열
  selectedDate?: Date;
  onDateSelect: (date: Date | undefined) => void;
  profileId: string;
};

export function DiarySidebar({
  calendarDates,
  selectedDate,
  onDateSelect,
  profileId,
}: DiarySidebarProps) {
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
          profileId={profileId}
          className="w-full sm:hidden"
          size="lg"
        />
      </div>
    </div>
  );
}