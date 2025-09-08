import { Card, CardContent, CardFooter, CardHeader } from "../card";
import { Skeleton } from "./skeleton";
import { cn } from "~/lib/utils";

interface DiaryCardSkeletonProps {
  className?: string;
  variant?: "pulse" | "shimmer";
}

export function DiaryCardSkeleton({ 
  className, 
  variant = "pulse" 
}: DiaryCardSkeletonProps) {
  return (
    <Card
      className={cn(
        "flex flex-col h-full",
        variant === "pulse" && "animate-pulse",
        className
      )}
      role="status"
      aria-label="일기 카드 로딩 중"
    >
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="space-y-2">
              {/* 날짜 제목 스켈레톤 */}
              <Skeleton className="h-6 w-20" variant={variant} />
              {/* 요일 스켈레톤 */}
              <Skeleton className="h-4 w-16" variant={variant} />
            </div>
          </div>
          {/* 더보기 버튼 스켈레톤 */}
          <Skeleton className="w-8 h-8" variant={variant} />
        </div>
      </CardHeader>

      <CardContent className="space-y-4 flex-grow flex flex-col">
        <div className="flex-grow space-y-4">
          {/* 감정 태그들 스켈레톤 */}
          <div className="flex flex-wrap gap-2">
            <Skeleton className="h-6 rounded-full w-16" variant={variant} />
            <Skeleton className="h-6 rounded-full w-20" variant={variant} />
            <Skeleton className="h-6 rounded-full w-14" variant={variant} />
          </div>

          {/* 내용 미리보기 스켈레톤 */}
          <div className="space-y-2">
            {/* 제목 줄 */}
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" variant={variant} />
              <Skeleton className="h-4 w-3/4" variant={variant} />
            </div>
            {/* 상황 내용 줄들 */}
            <div className="space-y-2 mt-2">
              <Skeleton className="h-3 w-full" variant={variant} />
              <Skeleton className="h-3 w-5/6" variant={variant} />
              <Skeleton className="h-3 w-2/3" variant={variant} />
            </div>
          </div>
        </div>

        {/* 작성 진행도 스켈레톤 */}
        <div className="flex items-center gap-3 pt-4">
          <div className="flex items-center gap-2">
            {/* 완료 체크 아이콘 스켈레톤 */}
            <Skeleton className="w-4 h-4 rounded-full" variant={variant} />
            {/* 진행도 텍스트 스켈레톤 */}
            <Skeleton className="h-3 w-12" variant={variant} />
          </div>
          {/* 진행도 바 스켈레톤 */}
          <div className="flex-1">
            <div className="w-full bg-muted rounded-full h-2">
              <Skeleton className="h-2 rounded-full w-3/5" variant={variant} />
            </div>
          </div>
          {/* 퍼센트 텍스트 스켈레톤 */}
          <Skeleton className="h-3 w-8" variant={variant} />
        </div>
      </CardContent>

      <CardFooter className="pt-3 border-t">
        <div className="flex gap-2 w-full">
          {/* 보기 버튼 스켈레톤 */}
          <Skeleton className="h-8 flex-1" variant={variant} />
          {/* 수정/계속쓰기 버튼 스켈레톤 */}
          <Skeleton className="h-8 flex-1" variant={variant} />
        </div>
      </CardFooter>
    </Card>
  );
}