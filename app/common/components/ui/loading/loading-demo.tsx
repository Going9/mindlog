/**
 * 로딩 스켈레톤 컴포넌트들의 사용 예시를 보여주는 데모 컴포넌트
 * 
 * 이 파일은 개발자가 로딩 스켈레톤 컴포넌트들을 어떻게 사용하는지
 * 확인할 수 있도록 만든 예시입니다. 프로덕션에서는 필요 없습니다.
 */

import { useState } from "react";
import { Button } from "../button";
import { 
  DiaryCardSkeleton, 
  DiaryListSkeleton, 
  LoadingSpinner,
  Skeleton 
} from "./index";

export function LoadingDemo() {
  const [showSkeleton, setShowSkeleton] = useState(true);
  const [variant, setVariant] = useState<"pulse" | "shimmer">("pulse");

  return (
    <div className="p-8 space-y-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-2">로딩 스켈레톤 데모</h1>
        <p className="text-muted-foreground mb-6">
          일기 앱에서 사용되는 로딩 스켈레톤 컴포넌트들의 예시입니다.
        </p>

        {/* 컨트롤 버튼들 */}
        <div className="flex gap-4 mb-8">
          <Button 
            onClick={() => setShowSkeleton(!showSkeleton)}
            variant="outline"
          >
            {showSkeleton ? "스켈레톤 숨기기" : "스켈레톤 보이기"}
          </Button>
          <Button
            onClick={() => setVariant(variant === "pulse" ? "shimmer" : "pulse")}
            variant="outline"
          >
            애니메이션: {variant === "pulse" ? "Pulse" : "Shimmer"}
          </Button>
        </div>

        {/* LoadingSpinner 예시 */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold mb-4">LoadingSpinner</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 border rounded-lg">
            <div className="flex flex-col items-center gap-2">
              <LoadingSpinner size="sm" text="작은 스피너" />
              <code className="text-xs">size="sm"</code>
            </div>
            <div className="flex flex-col items-center gap-2">
              <LoadingSpinner size="default" text="기본 스피너" />
              <code className="text-xs">size="default"</code>
            </div>
            <div className="flex flex-col items-center gap-2">
              <LoadingSpinner size="lg" text="큰 스피너" />
              <code className="text-xs">size="lg"</code>
            </div>
          </div>
        </section>

        {/* 기본 Skeleton 예시 */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold mb-4">기본 Skeleton</h2>
          <div className="space-y-4 p-4 border rounded-lg">
            <div className="space-y-2">
              <h3 className="text-sm font-medium">다양한 크기</h3>
              <Skeleton className="h-4 w-full" variant={variant} />
              <Skeleton className="h-4 w-3/4" variant={variant} />
              <Skeleton className="h-4 w-1/2" variant={variant} />
            </div>
            <div className="space-y-2">
              <h3 className="text-sm font-medium">다양한 모양</h3>
              <div className="flex gap-2">
                <Skeleton className="h-12 w-12 rounded-full" variant={variant} />
                <Skeleton className="h-12 w-24 rounded-md" variant={variant} />
                <Skeleton className="h-12 w-32 rounded-lg" variant={variant} />
              </div>
            </div>
          </div>
        </section>

        {/* DiaryCardSkeleton 예시 */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold mb-4">DiaryCardSkeleton</h2>
          {showSkeleton ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              <DiaryCardSkeleton variant={variant} />
              <DiaryCardSkeleton variant={variant} />
              <DiaryCardSkeleton variant={variant} />
            </div>
          ) : (
            <p className="text-muted-foreground">스켈레톤이 숨겨졌습니다.</p>
          )}
        </section>

        {/* DiaryListSkeleton 예시 */}
        <section>
          <h2 className="text-xl font-semibold mb-4">DiaryListSkeleton</h2>
          {showSkeleton ? (
            <DiaryListSkeleton count={6} variant={variant} />
          ) : (
            <p className="text-muted-foreground">스켈레톤이 숨겨졌습니다.</p>
          )}
        </section>

        {/* 사용법 코드 예시 */}
        <section className="mt-12">
          <h2 className="text-xl font-semibold mb-4">사용법</h2>
          <div className="bg-muted p-4 rounded-lg">
            <pre className="text-sm overflow-auto">
              <code>{`// 기본 사용법
import { 
  DiaryListSkeleton, 
  DiaryCardSkeleton, 
  LoadingSpinner 
} from "~/common/components/ui/loading";

// 일기 목록 로딩 시
{isLoading ? (
  <DiaryListSkeleton count={6} variant="pulse" />
) : (
  <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
    {diaries.map(diary => <DiaryCard key={diary.id} entry={diary} />)}
  </div>
)}

// 단일 카드 로딩 시  
{isLoading ? (
  <DiaryCardSkeleton variant="shimmer" />
) : (
  <DiaryCard entry={diary} />
)}

// 버튼 로딩 시
<Button disabled={isLoading}>
  {isLoading ? (
    <LoadingSpinner size="sm" text="저장 중..." />
  ) : (
    "저장"
  )}
</Button>`}</code>
            </pre>
          </div>
        </section>
      </div>
    </div>
  );
}