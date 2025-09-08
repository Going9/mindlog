import { cn } from "~/lib/utils";

interface LoadingSpinnerProps {
  className?: string;
  size?: "sm" | "default" | "lg";
  text?: string;
}

export function LoadingSpinner({ 
  className, 
  size = "default", 
  text 
}: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: "w-4 h-4",
    default: "w-6 h-6",
    lg: "w-8 h-8",
  };

  const textSizeClasses = {
    sm: "text-sm",
    default: "text-base",
    lg: "text-lg",
  };

  return (
    <div
      className={cn(
        "flex items-center justify-center gap-2",
        className
      )}
      role="status"
      aria-label={text ? `${text}` : "로딩 중"}
    >
      <div
        className={cn(
          "animate-spin rounded-full border-2 border-muted border-t-foreground",
          sizeClasses[size]
        )}
      />
      {text && (
        <span className={cn("text-muted-foreground", textSizeClasses[size])}>
          {text}
        </span>
      )}
    </div>
  );
}