import { cn } from "~/lib/utils";

interface SkeletonProps {
  className?: string;
  variant?: "pulse" | "shimmer";
}

export function Skeleton({ 
  className, 
  variant = "pulse" 
}: SkeletonProps) {
  // shimmer 효과를 위한 더 복잡한 애니메이션은 일단 pulse로 대체
  // 필요시 글로벌 CSS에 @keyframes shimmer를 추가하여 사용 가능
  const isShimmer = variant === "shimmer";
  
  return (
    <div
      className={cn(
        "bg-muted rounded",
        isShimmer 
          ? "animate-pulse opacity-75" // shimmer 대신 약간 다른 pulse 효과
          : "animate-pulse",
        className
      )}
      role="status"
      aria-hidden="true"
    />
  );
}