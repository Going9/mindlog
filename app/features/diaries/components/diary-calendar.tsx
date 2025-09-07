// src/app/features/diaries/components/diary-calendar.tsx

/**
 * ## 컴포넌트 흐름 및 역할
 *
 * 이 컴포넌트는 일기 목록 페이지의 사이드바에 위치하는 캘린더 UI를 담당합니다.
 * 주요 역할은 다음과 같습니다:
 * 1. 전체 일기 목록(`entries`)을 받아서 일기가 작성된 날짜를 캘린더에 시각적으로 표시합니다 (점 표시).
 * 2. 사용자가 특정 날짜를 클릭하면, 해당 날짜 정보를 상위 컴포넌트(`DiaryListPage`)로 전달하여 해당 날짜의 일기만 필터링되도록 합니다.
 * 3. 선택된 날짜에 어떤 일기가 있는지(감정 태그 요약) 간략하게 보여줍니다.
 *
 * ## 코드 구조 및 원리
 *
 * - 'DiaryCalendarProps' 인터페이스를 통해 부모 컴포넌트로부터 어떤 데이터를 받아야 하는지 명확히 정의합니다 (타입스크립트의 장점).
 * - 'entriesByDate' 객체: `reduce` 메소드를 사용하여 일기 배열을 날짜별로 그룹화된 객체로 변환합니다. 이는 특정 날짜에 일기가 있는지, 어떤 일기가 있는지 빠르게 찾기 위함이며, 캘린더를 렌더링할 때마다 전체 배열을 순회하는 것을 방지하여 성능을 향상시킵니다.
 * - `shadcn/ui`의 'Calendar' 컴포넌트를 기반으로 커스터마이징하여 사용합니다.
 *   - `modifiers`: 특정 조건을 만족하는 날짜에 스타일을 적용하는 기능입니다. 여기서는 `hasEntry`라는 modifier를 만들어 일기가 있는 날짜에 점을 찍는 데 사용합니다.
 *   - `onSelect`: 사용자가 날짜를 선택했을 때 호출되는 콜백 함수입니다. 이를 통해 부모 컴포넌트의 `selectedDate` 상태를 업데이트합니다.
 * - 하단에는 선택된 날짜의 정보를 표시하는 UI가 있으며, 해당 날짜에 작성된 일기가 있는지에 따라 다른 내용을 보여줍니다.
 */

import { useState } from "react";
import { Calendar } from "~/common/components/ui/calendar";
import { Card, CardContent } from "~/common/components/ui/card";

// --- 타입 정의 (Type Definitions) ---
// 이제 날짜 배열만 사용하므로 복잡한 타입 정의가 필요 없음

// 컴포넌트가 부모로부터 받아야 할 props(속성)를 정의합니다.
interface DiaryCalendarProps {
  diaryDates: string[]; // 일기가 있는 날짜 문자열 배열
  selectedDate?: Date; // 현재 선택된 날짜
  onDateSelect: (date: Date | undefined) => void; // 날짜를 선택했을 때 부모에게 알리는 함수
}

export function DiaryCalendar({
  diaryDates,
  selectedDate,
  onDateSelect,
}: DiaryCalendarProps) {
  // 현재 캘린더가 보여주는 월(month)을 관리하는 상태
  const [currentMonth, setCurrentMonth] = useState(new Date());

  // 일기가 있는 날짜를 Set으로 변환해서 빠른 조회를 위해 저장
  const diaryDateSet = new Set(diaryDates);

  // 특정 날짜에 일기가 있는지 확인하는 함수
  const hasEntry = (date: Date) => {
    const dateString = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; // YYYY-MM-DD 형태로 변환
    return diaryDateSet.has(dateString);
  };

  return (
    <Card className='w-full'>
      <CardContent className='flex flex-col items-center p-2 md:p-4'>
        <Calendar
          mode='single'
          selected={selectedDate}
          onSelect={onDateSelect}
          month={currentMonth}
          onMonthChange={setCurrentMonth}
          className='[&_.rdp-day]:w-9 [&_.rdp-day]:h-9'
          modifiers={{
            hasEntry: date => hasEntry(date),
          }}
          modifiersStyles={{
            hasEntry: {
              fontWeight: "bold",
            },
          }}
          modifiersClassNames={{
            hasEntry:
              "relative after:content-[''] after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:w-1.5 after:h-1.5 after:bg-primary after:rounded-full",
            selected:
              "bg-primary text-primary-foreground hover:bg-primary focus:bg-primary rounded-full",
          }}
        />
        {selectedDate && (
          <div className='mt-4 p-3 bg-muted/50 rounded-lg w-full'>
            <div className='flex items-center justify-center mb-2'>
              <h4 className='font-medium text-sm'>
                {selectedDate.toLocaleDateString("ko-KR", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                  weekday: "long",
                })}
              </h4>
            </div>

            {hasEntry(selectedDate) ? (
              <p className='text-xs text-muted-foreground text-center'>
                이 날에 작성된 일기가 있습니다.
              </p>
            ) : (
              <p className='text-xs text-muted-foreground text-center'>
                이 날에 작성된 일기가 없습니다.
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}