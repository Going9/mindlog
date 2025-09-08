# Loading 스켈레톤 UI 컴포넌트들

일기 앱의 디자인 시스템에 맞는 로딩 스켈레톤 UI 컴포넌트들입니다.

## 컴포넌트 목록

### 1. DiaryCardSkeleton
기존 DiaryCard와 동일한 레이아웃의 스켈레톤 컴포넌트

```tsx
import { DiaryCardSkeleton } from "~/common/components/ui/loading";

<DiaryCardSkeleton variant="pulse" />
```

### 2. DiaryListSkeleton  
여러 개의 DiaryCardSkeleton을 그리드로 배치한 목록 스켈레톤

```tsx
import { DiaryListSkeleton } from "~/common/components/ui/loading";

<DiaryListSkeleton count={6} variant="pulse" />
```

### 3. LoadingSpinner
간단한 원형 스피너 컴포넌트

```tsx
import { LoadingSpinner } from "~/common/components/ui/loading";

<LoadingSpinner size="default" text="로딩 중..." />
```

### 4. Skeleton
기본 스켈레톤 컴포넌트 (다른 컴포넌트들의 기반)

```tsx
import { Skeleton } from "~/common/components/ui/loading";

<Skeleton className="h-4 w-full" variant="pulse" />
```

## Props

### DiaryCardSkeleton
- `className?`: string - 추가 CSS 클래스
- `variant?`: "pulse" | "shimmer" - 애니메이션 종류 (기본: "pulse")

### DiaryListSkeleton  
- `className?`: string - 추가 CSS 클래스
- `count?`: number - 표시할 카드 개수 (기본: 6)
- `variant?`: "pulse" | "shimmer" - 애니메이션 종류 (기본: "pulse")

### LoadingSpinner
- `className?`: string - 추가 CSS 클래스  
- `size?`: "sm" | "default" | "lg" - 스피너 크기 (기본: "default")
- `text?`: string - 스피너와 함께 표시할 텍스트

### Skeleton
- `className?`: string - 추가 CSS 클래스
- `variant?`: "pulse" | "shimmer" - 애니메이션 종류 (기본: "pulse")

## 사용 예시

### 일기 목록 페이지에서 로딩 상태 표시

```tsx
export default function DiaryListPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [diaries, setDiaries] = useState([]);

  if (isLoading) {
    return (
      <div className="container mx-auto max-w-7xl px-4 py-8">
        <DiaryListSkeleton count={6} variant="pulse" />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      {diaries.map(diary => (
        <DiaryCard key={diary.id} entry={diary} />
      ))}
    </div>
  );
}
```

### 개별 카드 로딩

```tsx
function DiaryCardWrapper({ diaryId }: { diaryId: number }) {
  const [isLoading, setIsLoading] = useState(true);
  const [diary, setDiary] = useState(null);

  return (
    <>
      {isLoading ? (
        <DiaryCardSkeleton />
      ) : (
        <DiaryCard entry={diary} />
      )}
    </>
  );
}
```

### 버튼 로딩 상태

```tsx
function SaveButton({ onSave, isLoading }: SaveButtonProps) {
  return (
    <Button onClick={onSave} disabled={isLoading}>
      {isLoading ? (
        <LoadingSpinner size="sm" text="저장 중..." />
      ) : (
        "저장"
      )}
    </Button>
  );
}
```

### ConditionalDiaryButton과 동일한 스타일

기존 ConditionalDiaryButton의 로딩 상태와 동일한 스타일을 적용:

```tsx
<Button disabled size="lg">
  <LoadingSpinner size="sm" text="확인 중..." />
</Button>
```

## 디자인 특징

- **기존 카드와 동일한 크기**: DiaryCard와 완전히 동일한 레이아웃 구조
- **shadcn/ui 스타일 준수**: 기존 디자인 시스템과 일관성 유지
- **접근성 고려**: `role="status"`, `aria-label` 속성 포함
- **부드러운 애니메이션**: pulse 애니메이션으로 자연스러운 로딩 효과
- **반응형 디자인**: 기존 그리드 레이아웃과 완벽 호환

## 접근성

모든 스켈레톤 컴포넌트는 다음 접근성 속성을 포함합니다:

- `role="status"`: 화면 리더에게 로딩 상태임을 알림
- `aria-label`: 적절한 한국어 라벨 제공
- `aria-hidden="true"`: 장식적인 요소에는 숨김 처리

## 파일 구조

```
app/common/components/ui/loading/
├── diary-card-skeleton.tsx    # DiaryCard 스켈레톤
├── diary-list-skeleton.tsx    # DiaryCard 목록 스켈레톤  
├── loading-spinner.tsx        # 원형 로딩 스피너
├── skeleton.tsx               # 기본 스켈레톤 컴포넌트
├── loading-demo.tsx          # 사용 예시 데모 (개발용)
├── index.ts                   # 컴포넌트 export
└── README.md                  # 이 파일
```

## 개발자 노트

- `variant="shimmer"`는 현재 `pulse`와 동일하게 작동합니다. 
- 실제 shimmer 효과가 필요한 경우 글로벌 CSS에 키프레임을 추가할 수 있습니다.
- 모든 컴포넌트는 TypeScript로 작성되어 타입 안정성을 보장합니다.
- 성능을 위해 불필요한 re-render를 방지하는 구조로 설계되었습니다.