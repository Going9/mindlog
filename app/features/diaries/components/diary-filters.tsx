/**
 * 일기 목록 필터링 및 정렬 컴포넌트
 * 
 * 제어 컴포넌트 패턴을 사용하여 모든 상태를 부모에서 관리
 * - 검색, 정렬, 감정 필터, 작성 상태 필터 제공
 * - 활성 필터 시각적 표시 및 개별 제거 기능
 */

import { Button } from "~/common/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/common/components/ui/select";
import { Input } from "~/common/components/ui/input";
import { Badge } from "~/common/components/ui/badge";
import {
  SearchIcon,
  XIcon,
  SortAscIcon,
  SortDescIcon,
} from "lucide-react";

interface EmotionTag {
  id: number;
  name: string;
  color: string | null;
  category: "positive" | "negative" | "neutral" | null;
  isDefault: boolean | null;
  usageCount?: number | null;
}

interface DiaryFiltersProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  sortBy: string;
  onSortChange: (sort: string) => void;
  selectedEmotionFilter?: EmotionTag;
  onEmotionFilterChange: (emotion?: EmotionTag) => void;
  completionFilter: string;
  onCompletionFilterChange: (completion: string) => void;
  availableEmotions: EmotionTag[];
  onClearFilters: () => void;
  totalEntries: number;
  filteredEntries: number;
}

const COMPLETION_LABELS = {
  all: "전체",
  complete: "완료된 일기",
  incomplete: "미완료 일기",
} as const;

const SORT_OPTIONS = [
  { value: "date-desc", label: "최신순" },
  { value: "date-asc", label: "오래된순" },
  { value: "completion-desc", label: "완성도 높은 순" },
  { value: "completion-asc", label: "완성도 낮은 순" },
] as const;

const COMPLETION_OPTIONS = [
  { value: "all", label: "전체" },
  { value: "complete", label: "완료됨" },
  { value: "incomplete", label: "미완료" },
] as const;

export function DiaryFilters(props: DiaryFiltersProps) {
  const {
    searchQuery,
    onSearchChange,
    sortBy,
    onSortChange,
    selectedEmotionFilter,
    onEmotionFilterChange,
    completionFilter,
    onCompletionFilterChange,
    availableEmotions,
    onClearFilters,
    totalEntries,
    filteredEntries,
  } = props;

  const hasActiveFilters = Boolean(
    searchQuery ||
    selectedEmotionFilter ||
    completionFilter !== "all" ||
    sortBy !== "date-desc"
  );

  const handleEmotionFilterChange = (value: string) => {
    if (value === "all") {
      onEmotionFilterChange(undefined);
    } else {
      const emotion = availableEmotions.find(e => e.id.toString() === value);
      onEmotionFilterChange(emotion);
    }
  };

  const getCompletionLabel = (value: string) => {
    return COMPLETION_LABELS[value as keyof typeof COMPLETION_LABELS] || "완성도";
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row gap-3">
        <SearchInput 
          value={searchQuery}
          onChange={onSearchChange}
        />
        <SortSelector
          value={sortBy}
          onChange={onSortChange}
        />
      </div>

      <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
        <EmotionFilter
          selectedEmotion={selectedEmotionFilter}
          availableEmotions={availableEmotions}
          onChange={handleEmotionFilterChange}
        />
        <CompletionFilter
          value={completionFilter}
          onChange={onCompletionFilterChange}
        />
        {hasActiveFilters && (
          <ClearFiltersButton onClick={onClearFilters} />
        )}
      </div>

      {hasActiveFilters && (
        <ActiveFilters
          searchQuery={searchQuery}
          selectedEmotionFilter={selectedEmotionFilter}
          completionFilter={completionFilter}
          onSearchChange={onSearchChange}
          onEmotionFilterChange={onEmotionFilterChange}
          onCompletionFilterChange={onCompletionFilterChange}
          getCompletionLabel={getCompletionLabel}
        />
      )}

      <FilterSummary
        totalEntries={totalEntries}
        filteredEntries={filteredEntries}
        hasActiveFilters={hasActiveFilters}
      />
    </div>
  );
}

// 서브 컴포넌트들
function SearchInput({ value, onChange }: { value: string; onChange: (query: string) => void }) {
  return (
    <div className="relative flex-1 max-w-sm">
      <SearchIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
      <Input
        placeholder="일기 내용 검색..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="pl-10"
      />
    </div>
  );
}

function SortSelector({ value, onChange }: { value: string; onChange: (sort: string) => void }) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-full sm:w-[180px]">
        <div className="flex items-center gap-2">
          {value.includes("desc") ? (
            <SortDescIcon className="w-4 h-4" />
          ) : (
            <SortAscIcon className="w-4 h-4" />
          )}
          <SelectValue />
        </div>
      </SelectTrigger>
      <SelectContent>
        {SORT_OPTIONS.map((option) => (
          <SelectItem key={option.value} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function EmotionFilter({ 
  selectedEmotion, 
  availableEmotions, 
  onChange 
}: { 
  selectedEmotion?: EmotionTag; 
  availableEmotions: EmotionTag[]; 
  onChange: (value: string) => void;
}) {
  return (
    <Select
      value={selectedEmotion?.id.toString() || "all"}
      onValueChange={onChange}
    >
      <SelectTrigger className="w-full sm:w-[160px]">
        <SelectValue placeholder="감정별 보기" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">전체 감정</SelectItem>
        {availableEmotions.map((emotion) => (
          <SelectItem key={emotion.id} value={emotion.id.toString()}>
            <div className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: emotion.color || undefined }}
              />
              {emotion.name}
            </div>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function CompletionFilter({ value, onChange }: { value: string; onChange: (completion: string) => void }) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-full sm:w-[140px]">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {COMPLETION_OPTIONS.map((option) => (
          <SelectItem key={option.value} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function ClearFiltersButton({ onClick }: { onClick: () => void }) {
  return (
    <Button
      variant="outline"
      size="sm"
      onClick={onClick}
      className="flex items-center gap-2"
    >
      <XIcon className="w-4 h-4" />
      필터 초기화
    </Button>
  );
}

function ActiveFilters({
  searchQuery,
  selectedEmotionFilter,
  completionFilter,
  onSearchChange,
  onEmotionFilterChange,
  onCompletionFilterChange,
  getCompletionLabel,
}: {
  searchQuery: string;
  selectedEmotionFilter?: EmotionTag;
  completionFilter: string;
  onSearchChange: (query: string) => void;
  onEmotionFilterChange: (emotion?: EmotionTag) => void;
  onCompletionFilterChange: (completion: string) => void;
  getCompletionLabel: (value: string) => string;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-muted-foreground">활성 필터:</span>

      {searchQuery && (
        <Badge variant="secondary" className="flex items-center gap-1">
          검색: "{searchQuery}"
          <Button
            variant="ghost"
            size="icon"
            className="h-4 w-4 hover:bg-transparent"
            onClick={() => onSearchChange("")}
          >
            <XIcon className="w-3 h-3" />
          </Button>
        </Badge>
      )}

      {selectedEmotionFilter && (
        <Badge
          style={{ backgroundColor: selectedEmotionFilter.color || undefined }}
          className="text-white flex items-center gap-1"
        >
          {selectedEmotionFilter.name}
          <Button
            variant="ghost"
            size="icon"
            className="h-4 w-4 hover:bg-white/20"
            onClick={() => onEmotionFilterChange(undefined)}
          >
            <XIcon className="w-3 h-3" />
          </Button>
        </Badge>
      )}

      {completionFilter !== "all" && (
        <Badge variant="secondary" className="flex items-center gap-1">
          {getCompletionLabel(completionFilter)}
          <Button
            variant="ghost"
            size="icon"
            className="h-4 w-4 hover:bg-transparent"
            onClick={() => onCompletionFilterChange("all")}
          >
            <XIcon className="w-3 h-3" />
          </Button>
        </Badge>
      )}
    </div>
  );
}

function FilterSummary({
  totalEntries,
  filteredEntries,
  hasActiveFilters,
}: {
  totalEntries: number;
  filteredEntries: number;
  hasActiveFilters: boolean;
}) {
  return (
    <div className="flex items-center justify-between text-sm text-muted-foreground">
      <span>
        {filteredEntries === totalEntries
          ? `총 ${totalEntries}개의 일기`
          : `${filteredEntries}개 일기 (전체 ${totalEntries}개 중)`}
      </span>
      {hasActiveFilters && (
        <span className="text-primary">
          필터 적용됨
        </span>
      )}
    </div>
  );
}
