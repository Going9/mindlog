// src/app/features/diaries/components/emotion-tag-selector.tsx

/**
 * ## 컴포넌트 흐름 및 역할
 *
 * 이 컴포넌트는 사용자가 자신의 감정을 나타내는 태그를 선택할 수 있는 UI를 제공합니다.
 * 주요 기능은 다음과 같습니다:
 * 1. 미리 정의된 기본 감정 태그들을 긍정/부정/중립 카테고리별로 보여줍니다.
 * 2. 사용자가 태그를 클릭하여 선택하거나 선택 해제할 수 있습니다.
 * 3. 사용자가 자신만의 새로운 감정 태그를 직접 만들 수 있는 기능을 제공합니다.
 * 4. 선택된 태그 목록을 상위 컴포넌트(`SteppedDiaryForm`)로 전달합니다.
 *
 * ## 코드 구조 및 원리
 *
 * - **자체 상태와 부모로부터 받은 상태의 조합**:
 *   - `selectedTags`, `onTagsChange`: 어떤 태그가 선택되었는지에 대한 정보와 이를 변경하는 함수는 부모로부터 props로 받습니다. (제어 컴포넌트 패턴)
 *   - `customTags`, `newTagName`, `newTagCategory`, `isAddingTag`: 사용자가 새로운 태그를 만드는 과정에 필요한 상태는 이 컴포넌트가 자체적으로 `useState`를 통해 관리합니다. 이는 "새 태그 만들기" 기능이 이 컴포넌트 내에서만 일어나기 때문입니다.
 *
 * - **데이터 처리**:
 *   - `allTags`: 기본 태그와 사용자가 만든 커스텀 태그를 합쳐서 전체 태그 목록을 만듭니다.
 *   - `groupedTags`: 전체 태그 목록을 `positive`, `negative`, `neutral` 카테고리별로 그룹화하여 화면에 표시하기 좋게 데이터를 재구성합니다.
 *
 * - **이벤트 핸들러**:
 *   - `toggleTag`: 태그를 클릭했을 때, 이미 선택된 태그이면 목록에서 제거하고, 아니면 추가합니다. 변경된 최종 목록을 `onTagsChange` 함수를 통해 부모에게 알립니다.
 *   - `createCustomTag`: 사용자가 입력한 이름과 카테고리로 새로운 태그 객체를 만들어 `customTags` 상태에 추가하고, 동시에 선택된 태그 목록에도 추가합니다.
 */

import { useState, useEffect } from "react";
import { Button } from "~/common/components/ui/button";
import { Input } from "~/common/components/ui/input";
import { Label } from "~/common/components/ui/label";
import { Badge } from "~/common/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/common/components/ui/select";
import { Separator } from "~/common/components/ui/separator";
import { PlusIcon, XIcon } from "lucide-react";

// --- 타입 정의 ---
interface EmotionTag {
  id: number;
  name: string;
  color: string;
  category: "positive" | "negative" | "neutral";
  isDefault: boolean;
}

// 부모로부터 받아야 할 props 정의
interface EmotionTagSelectorProps {
  selectedTags: EmotionTag[]; // 현재 선택된 태그 목록
  onTagsChange: (tags: EmotionTag[]) => void; // 태그 목록이 변경될 때 부모에게 알리는 함수
  profileId: string; // 사용자 프로필 ID
}


export function EmotionTagSelector({
  selectedTags,
  onTagsChange,
  profileId,
}: EmotionTagSelectorProps) {
  // --- 자체 상태 관리 ---
  const [allTags, setAllTags] = useState<EmotionTag[]>([]); // 데이터베이스에서 가져온 모든 태그 목록
  const [newTagName, setNewTagName] = useState(""); // 새로 만들 태그의 이름 입력값
  const [newTagCategory, setNewTagCategory] = useState<"positive" | "negative" | "neutral">("neutral"); // 새로 만들 태그의 카테고리
  const [isAddingTag, setIsAddingTag] = useState(false); // 태그 추가 UI를 보여줄지 여부
  const [isLoading, setIsLoading] = useState(true); // 태그 로딩 상태
  const [isCreatingTag, setIsCreatingTag] = useState(false); // 커스텀 태그 생성 중 상태

  // 컴포넌트 마운트 시 기본 감정 태그 설정 (임시 해결책)
  useEffect(() => {
    // 임시로 기본 태그만 사용
    const defaultTags = [
      { id: 1, name: "기쁨", color: "#10B981", category: "positive" as const, isDefault: true },
      { id: 2, name: "행복", color: "#3B82F6", category: "positive" as const, isDefault: true },
      { id: 3, name: "감사", color: "#8B5CF6", category: "positive" as const, isDefault: true },
      { id: 4, name: "설렘", color: "#F59E0B", category: "positive" as const, isDefault: true },
      { id: 5, name: "슬픔", color: "#6B7280", category: "negative" as const, isDefault: true },
      { id: 6, name: "분노", color: "#EF4444", category: "negative" as const, isDefault: true },
      { id: 7, name: "불안", color: "#F97316", category: "negative" as const, isDefault: true },
      { id: 8, name: "걱정", color: "#84CC16", category: "negative" as const, isDefault: true },
      { id: 9, name: "평온", color: "#06B6D4", category: "neutral" as const, isDefault: true },
      { id: 10, name: "무관심", color: "#64748B", category: "neutral" as const, isDefault: true },
    ];
    
    setAllTags(defaultTags);
    setIsLoading(false);
  }, [profileId]);

  // --- 이벤트 핸들러 ---
  const toggleTag = (tag: EmotionTag) => {
    const isSelected = selectedTags.find(t => t.id === tag.id);
    if (isSelected) {
      onTagsChange(selectedTags.filter(t => t.id !== tag.id));
    } else {
      onTagsChange([...selectedTags, tag]);
    }
  };

  const createCustomTag = async () => {
    // 임시로 커스텀 태그 기능 비활성화
    alert("현재 커스텀 태그 기능이 일시적으로 비활성화되어 있습니다. 기본 태그를 사용해주세요.");
  };

  // --- 헬퍼 함수 ---
  const getTagVariant = (tag: EmotionTag) => {
    return selectedTags.some(t => t.id === tag.id) ? "default" : "secondary";
  };

  const getCategoryLabel = (category: string) => {
    switch (category) {
      case "positive": return "긍정적";
      case "negative": return "부정적";
      default: return "중립적";
    }
  };

  const groupedTags = {
    positive: allTags.filter(tag => tag.category === "positive"),
    negative: allTags.filter(tag => tag.category === "negative"),
    neutral: allTags.filter(tag => tag.category === "neutral"),
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="flex justify-center items-center py-8">
          <div className="text-muted-foreground">감정 태그를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className='space-y-4'>
      {selectedTags.length > 0 && (
        <div className='space-y-3 p-4 bg-muted/80 rounded-lg'>
          <Label className='text-sm font-medium flex items-center gap-2'>선택된 감정</Label>
          <div className='flex flex-wrap gap-2'>
            {selectedTags.map(tag => (
              <Badge
                key={tag.id}
                variant='default'
                style={{ backgroundColor: tag.color }}
                className='text-white cursor-pointer hover:opacity-80 hover:scale-105 transition-all duration-200 h-8 text-sm'
                onClick={() => toggleTag(tag)}
              >
                {tag.name}
                <XIcon className='w-3 h-3 ml-1 hover:rotate-90 transition-transform' />
              </Badge>
            ))}
          </div>
        </div>
      )}

      <div className='space-y-6'>
        {Object.entries(groupedTags).map(([category, tags], index) => (
          <div key={category}>
            {index > 0 && <Separator className='my-4' />}
            <div className='space-y-4'>
              <div className='flex items-center gap-2'><h4 className='text-sm font-semibold text-foreground'>{getCategoryLabel(category)} 감정</h4></div>
              <div className='flex flex-wrap gap-2'>
                {tags.map(tag => (
                  <Badge
                    key={tag.id}
                    variant={getTagVariant(tag)}
                    style={{ backgroundColor: selectedTags.find(t => t.id === tag.id) ? tag.color : "transparent", borderColor: tag.color, color: selectedTags.find(t => t.id === tag.id) ? "white" : tag.color, }}
                    className='cursor-pointer hover:opacity-80 hover:scale-105 transition-all duration-200 border h-8 text-sm min-h-[32px] min-w-[44px] flex items-center justify-center'
                    onClick={() => toggleTag(tag)}
                  >
                    {tag.name}
                    {!tag.isDefault && <span className='ml-1 text-xs opacity-70'>⭐</span>}
                  </Badge>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className='space-y-3 pt-4 border-t'>
        <Label className='text-sm font-medium'>새로운 감정 태그 만들기</Label>
        {isAddingTag ? (
          <div className='space-y-3'>
            <div className='flex gap-2'>
              <Input value={newTagName} onChange={e => setNewTagName(e.target.value)} placeholder='감정 이름 입력...' onKeyDown={e => { if (e.key === "Enter") { createCustomTag(); } else if (e.key === "Escape") { setIsAddingTag(false); setNewTagName(""); setNewTagCategory("neutral"); } }} className='flex-1 placeholder:text-xs sm:placeholder:text-sm' />
              <Select value={newTagCategory} onValueChange={(value: "positive" | "negative" | "neutral") => setNewTagCategory(value)}>
                <SelectTrigger className='w-32'><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value='positive'>긍정적</SelectItem>
                  <SelectItem value='negative'>부정적</SelectItem>
                  <SelectItem value='neutral'>중립적</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className='flex gap-2'>
              <Button 
                size='sm' 
                onClick={createCustomTag} 
                disabled={!newTagName.trim() || isCreatingTag} 
                className='flex-1'
              >
                {isCreatingTag ? "생성 중..." : "추가"}
              </Button>
              <Button 
                size='sm' 
                variant='outline' 
                onClick={() => { 
                  setIsAddingTag(false); 
                  setNewTagName(""); 
                  setNewTagCategory("neutral"); 
                }} 
                disabled={isCreatingTag}
                className='flex-1'
              >
                취소
              </Button>
            </div>
          </div>
        ) : (
          <Button variant='outline' size='sm' onClick={() => setIsAddingTag(true)} className='w-full'><PlusIcon className='w-4 h-4 mr-2' />나만의 감정 태그 만들기</Button>
        )}
      </div>
    </div>
  );
}
