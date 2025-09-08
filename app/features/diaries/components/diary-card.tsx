// src/app/features/diaries/components/diary-card.tsx

/**
 * ## 컴포넌트 흐름 및 역할
 *
 * 이 컴포넌트는 일기 목록 페이지에서 개별 일기 항목 하나를 시각적으로 표시하는 "카드" UI입니다.
 * 각 카드는 일기의 핵심 정보(날짜, 내용 요약, 감정 태그, 작성 진행도)를 한눈에 보여줍니다.
 * 또한, 사용자가 카드를 통해 일기를 보거나, 수정하거나, 삭제할 수 있는 상호작용을 제공합니다.
 *
 * ## 코드 구조 및 원리
 *
 * - **상태 끌어올리기 (Lifting State Up) 패턴**:
 *   이 컴포넌트는 자체적으로 상태를 가지지 않습니다. 대신, 모든 데이터(`entry`)와
 *   이벤트 핸들러(`onEdit`, `onDelete`, `onView`)를 부모 컴포넌트(`DiaryListPage`)로부터
 *   props로 전달받습니다. 사용자가 "수정" 버튼을 누르면, 이 컴포넌트는 부모로부터 받은
 *   `onEdit` 함수를 호출할 뿐, 실제 수정 로직을 알지 못합니다. 수정 로직은 부모가
 *   알아서 처리합니다. 이 패턴은 컴포넌트를 재사용 가능하고 예측 가능하게 만듭니다.
 *
 * - `DiaryCardProps` 인터페이스는 이 컴포넌트가 필요로 하는 데이터와 함수들을 명확히 정의합니다.
 * - `shadcn/ui`의 `Card` 관련 컴포넌트들(`CardHeader`, `CardContent` 등)을 사용하여 카드의 전체적인
 *   구조를 잡습니다.
 * - `DropdownMenu` 컴포넌트는 "더보기"(...) 버튼을 눌렀을 때 나오는 메뉴(보기, 수정, 삭제)를 만듭니다.
 * - `Badge` 컴포넌트로 감정 태그를 시각적으로 표시하고, `div`와 `style`을
 *   사용하여 작성 진행도를 시각적인 바로 표현합니다.
 * - 카드의 모든 부분(헤더, 푸터의 버튼, 드롭다운 메뉴 아이템)에서 발생하는 클릭 이벤트는
 *   모두 부모로부터 받은 핸들러 함수를 호출하도록 연결되어 있습니다.
 */

import {
  CheckIcon,
  EditIcon,
  EyeIcon,
  Loader2Icon,
  MoreHorizontalIcon,
  TrashIcon,
} from "lucide-react";
import { Badge } from "~/common/components/ui/badge";
import { Button } from "~/common/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "~/common/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "~/common/components/ui/dropdown-menu";
import { cn } from "~/lib/utils"; // 여러 개의 CSS 클래스를 조건부로 합쳐주는 유틸리티 함수

// --- 타입 정의 ---
interface EmotionTag {
  id: number;
  name: string;
  color: string | null;
  category: "positive" | "negative" | "neutral" | null;
  isDefault: boolean | null;
}

interface DiaryEntry {
  id: number;
  date: Date;
  shortContent: string | null;
  situation: string | null;
  reaction: string | null;
  physicalSensation: string | null;
  desiredReaction: string | null;
  gratitudeMoment: string | null;
  selfKindWords: string | null;
  emotionTags: EmotionTag[];
  completedSteps: number;
  totalSteps: number;
}

// 부모로부터 받아야 할 props(속성) 정의
interface DiaryCardProps {
  entry: DiaryEntry; // 표시할 일기 데이터 객체
  onEdit: (id: number) => void; // 수정 버튼 클릭 시 호출될 함수
  onDelete: (id: number) => void; // 삭제 버튼 클릭 시 호출될 함수
  onView: (id: number) => void; // 보기 버튼 클릭 시 호출될 함수
  isDeleting?: boolean; // 삭제 중인지 여부
}

export function DiaryCard({ entry, onEdit, onDelete, onView, isDeleting }: DiaryCardProps) {
  // props로 받은 entry 객체에서 필요한 값들을 구조 분해 할당으로 추출합니다.
  const {
    id,
    date,
    shortContent,
    situation,
    emotionTags,
    completedSteps,
    totalSteps,
  } = entry;

  // 작성 완료 여부와 진행도를 계산합니다.
  const isComplete = completedSteps === totalSteps;
  const completionPercentage =
    totalSteps > 0 ? (completedSteps / totalSteps) * 100 : 0;

  return (
    <Card className={cn(
      'hover:shadow-md transition-all duration-200 flex flex-col h-full',
      isDeleting && 'opacity-50 pointer-events-none'
    )}>
      <CardHeader className='pb-3'>
        <div className='flex items-start justify-between'>
          <div className='flex items-center gap-3'>
            <div>
              <CardTitle className='text-lg'>
                {date.toLocaleDateString("ko-KR", {
                  month: "long",
                  day: "numeric",
                })}
              </CardTitle>
              <CardDescription>
                {date.toLocaleDateString("ko-KR", { weekday: "long" })}
              </CardDescription>
            </div>
          </div>
          {/* 더보기(...) 버튼과 드롭다운 메뉴 */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant='ghost' size='icon' className='h-8 w-8'>
                <MoreHorizontalIcon className='w-4 h-4' />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align='end'>
              <DropdownMenuItem onClick={() => onView(id)}>
                <EyeIcon className='w-4 h-4 mr-2' />
                보기
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onEdit(id)}>
                <EditIcon className='w-4 h-4 mr-2' />
                수정
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => onDelete(id)}
                className='text-destructive focus:text-destructive'
                disabled={isDeleting}
              >
                {isDeleting ? (
                  <Loader2Icon className='w-4 h-4 mr-2 animate-spin' />
                ) : (
                  <TrashIcon className='w-4 h-4 mr-2' />
                )}
                {isDeleting ? '삭제 중...' : '삭제'}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>

      <CardContent className='space-y-4 flex-grow flex flex-col'>
        <div className='flex-grow'>
          {/* 감정 태그 */}
          {emotionTags.length > 0 && (
            <div className='flex flex-wrap gap-2'>
              {emotionTags.slice(0, 4).map(tag => (
                <Badge
                  key={tag.id}
                  style={{ backgroundColor: tag.color || "#6B7280" }}
                  className='text-white text-xs h-6 px-2'
                >
                  {tag.name}
                </Badge>
              ))}
              {emotionTags.length > 4 && (
                <Badge variant='secondary' className='text-xs h-6 px-2'>
                  +{emotionTags.length - 4}
                </Badge>
              )}
            </div>
          )}

          {/* 내용 미리보기 */}
          <div className='space-y-2 mt-4'>
            {shortContent && (
              <p className='font-medium text-sm leading-relaxed line-clamp-2 min-h-[2.5rem]'>
                {shortContent}
              </p>
            )}
            {situation && (
              <p className='text-muted-foreground text-sm leading-relaxed line-clamp-3'>
                {situation}
              </p>
            )}
          </div>
        </div>

        {/* 작성 진행도 */}
        <div className='flex items-center gap-3 pt-4'>
          <div className='flex items-center gap-2'>
            <div
              className={cn(
                "w-4 h-4 rounded-full flex items-center justify-center",
                isComplete ? "bg-green-500" : "bg-muted"
              )}
            >
              {isComplete && <CheckIcon className='w-3 h-3 text-white' />}
            </div>
            <span className='text-xs text-muted-foreground'>
              {isComplete ? "완료" : `${completedSteps}/${totalSteps} 단계`}
            </span>
          </div>
          <div className='flex-1'>
            <div className='w-full bg-muted rounded-full h-2'>
              <div
                className='bg-primary h-2 rounded-full transition-all'
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
          </div>
          <span className='text-xs text-muted-foreground font-medium'>
            {Math.round(completionPercentage)}%
          </span>
        </div>
      </CardContent>

      <CardFooter className='pt-3 border-t'>
        <div className='flex gap-2 w-full'>
          <Button
            variant='outline'
            size='sm'
            className='flex-1'
            onClick={() => onView(entry.id)}
          >
            <EyeIcon className='w-4 h-4 mr-2' />
            보기
          </Button>
          <Button
            variant='ghost'
            size='sm'
            className='flex-1'
            onClick={() => onEdit(id)}
          >
            <EditIcon className='w-4 h-4 mr-2' />
            {isComplete ? "수정" : "계속 쓰기"}
          </Button>
        </div>
      </CardFooter>
    </Card>
  );
}
