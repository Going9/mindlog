import { DiaryCardSkeleton } from "./diary-card-skeleton";
import { cn } from "~/lib/utils";

interface DiaryListSkeletonProps {
  className?: string;
  count?: number;
  variant?: "pulse" | "shimmer";
}

export function DiaryListSkeleton({ 
  className, 
  count = 6,
  variant = "pulse"
}: DiaryListSkeletonProps) {
  return (
    <div
      className={cn(
        "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6",
        className
      )}
      role="status"
      aria-label="일기 목록 로딩 중"
    >
      {Array.from({ length: count }).map((_, index) => (
        <DiaryCardSkeleton key={index} variant={variant} />
      ))}
    </div>
  );
}