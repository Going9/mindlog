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

import { useState, useEffect, useRef } from "react";
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
import { PlusIcon, XIcon, HeartIcon, FrownIcon, MehIcon, Trash2Icon } from "lucide-react";

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
  const [isDeletingTag, setIsDeletingTag] = useState<number | null>(null); // 삭제 중인 태그 ID
  const [longPressedTag, setLongPressedTag] = useState<number | null>(null); // 길게 누른 태그 ID
  const longPressTimer = useRef<NodeJS.Timeout | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<number | null>(null); // 삭제 확인 모달

  // 컴포넌트 마운트 시 감정 태그 데이터 로드
  useEffect(() => {
    const loadEmotionTags = async () => {
      try {
        const response = await fetch(`/api/emotion-tags/${profileId}`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAllTags(data.tags);
      } catch (error) {
        console.error("감정 태그 로딩 중 오류:", error);
        // 오류 발생 시 기본 태그라도 보여주기
        const fallbackTags = [
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
        setAllTags(fallbackTags);
      } finally {
        setIsLoading(false);
      }
    };

    loadEmotionTags();
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
    if (!newTagName.trim() || isCreatingTag) return;
    
    setIsCreatingTag(true);
    
    try {
      const getCategoryColor = (category: "positive" | "negative" | "neutral") => {
        switch (category) {
          case "positive": return "#10B981";
          case "negative": return "#EF4444";
          default: return "#6B7280";
        }
      };

      // 데이터베이스에 커스텀 태그 생성
      const response = await fetch("/api/create-emotion-tag", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          profileId,
          name: newTagName.trim(),
          color: getCategoryColor(newTagCategory),
          category: newTagCategory,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "태그 생성 중 오류가 발생했습니다.");
      }

      const data = await response.json();
      const formattedNewTag: EmotionTag = data.tag;

      // 로컬 태그 목록 업데이트
      setAllTags(prevTags => [...prevTags, formattedNewTag]);
      
      // 선택된 태그 목록에 추가
      onTagsChange([...selectedTags, formattedNewTag]);
      
      // 폼 초기화
      setNewTagName("");
      setNewTagCategory("neutral");
      setIsAddingTag(false);
    } catch (error) {
      console.error("커스텀 태그 생성 중 오류:", error);
      const errorMessage = error instanceof Error ? error.message : "태그 생성 중 오류가 발생했습니다.";
      alert(errorMessage);
    } finally {
      setIsCreatingTag(false);
    }
  };

  const deleteCustomTag = async (tagId: number, tagName: string) => {
    setIsDeletingTag(tagId);

    try {
      const response = await fetch("/api/delete-emotion-tag", {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          profileId,
          tagId: tagId,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "태그 삭제 중 오류가 발생했습니다.");
      }

      // 로컬 태그 목록에서 제거
      setAllTags(prevTags => prevTags.filter(tag => tag.id !== tagId));
      
      // 선택된 태그 목록에서도 제거 (만약 선택되어 있었다면)
      const updatedSelectedTags = selectedTags.filter(tag => tag.id !== tagId);
      if (updatedSelectedTags.length !== selectedTags.length) {
        onTagsChange(updatedSelectedTags);
      }

    } catch (error) {
      console.error("커스텀 태그 삭제 중 오류:", error);
      const errorMessage = error instanceof Error ? error.message : "태그 삭제 중 오류가 발생했습니다.";
      alert(errorMessage);
    } finally {
      setIsDeletingTag(null);
      setShowDeleteConfirm(null);
    }
  };

  // 길게 누르기 핸들러
  const handleLongPressStart = (tagId: number) => {
    longPressTimer.current = setTimeout(() => {
      setLongPressedTag(tagId);
      setShowDeleteConfirm(tagId);
      // 햅틱 피드백 (지원하는 경우)
      if ('vibrate' in navigator) {
        navigator.vibrate(50);
      }
    }, 500); // 500ms 길게 누르기
  };

  const handleLongPressEnd = () => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
    setLongPressedTag(null);
  };

  const handleTagClick = (tag: EmotionTag) => {
    // 삭제 확인 모드에서는 태그 선택/해제 방지
    if (showDeleteConfirm) return;
    toggleTag(tag);
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

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case "positive": return <HeartIcon className="w-4 h-4 text-green-600" />;
      case "negative": return <FrownIcon className="w-4 h-4 text-red-600" />;
      default: return <MehIcon className="w-4 h-4 text-gray-600" />;
    }
  };

  const getCategoryBgClass = (category: string) => {
    switch (category) {
      case "positive": return "bg-green-50 dark:bg-green-950/20 border-green-200 dark:border-green-800/30";
      case "negative": return "bg-red-50 dark:bg-red-950/20 border-red-200 dark:border-red-800/30";
      default: return "bg-gray-50 dark:bg-gray-950/20 border-gray-200 dark:border-gray-800/30";
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
    <div className='space-y-6'>
      {selectedTags.length > 0 && (
        <div className='space-y-3 p-4 bg-muted/80 rounded-lg border'>
          <Label className='text-sm font-medium flex items-center gap-2'>
            선택된 감정 ({selectedTags.length}개)
          </Label>
          <div className='flex flex-wrap gap-3'>
            {selectedTags.map(tag => (
              <Badge
                key={tag.id}
                variant='default'
                style={{ backgroundColor: tag.color }}
                className='text-white cursor-pointer hover:opacity-80 active:scale-95 transition-all duration-200 h-10 text-sm px-4 min-w-[60px] flex items-center justify-between gap-2 touch-manipulation'
                onClick={() => handleTagClick(tag)}
                role="button"
                tabIndex={0}
                aria-label={`${tag.name} 태그 선택 해제`}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleTagClick(tag);
                  }
                }}
              >
                <span>{tag.name}</span>
                <XIcon className='w-4 h-4 opacity-75' />
              </Badge>
            ))}
          </div>
        </div>
      )}

      <div className='space-y-4'>
        {Object.entries(groupedTags).map(([category, tags], index) => (
          <div key={category} className={`p-4 rounded-xl border-2 ${getCategoryBgClass(category)}`}>
            <div className='space-y-4'>
              <div className='flex items-center gap-2 mb-3'>
                {getCategoryIcon(category)}
                <h4 className='text-sm font-semibold text-foreground'>
                  {getCategoryLabel(category)} 감정 ({tags.length}개)
                </h4>
              </div>
              <div className='flex flex-wrap gap-3'>
                {tags.map(tag => {
                  const isSelected = selectedTags.find(t => t.id === tag.id);
                  const isCustomTag = !tag.isDefault;
                  const isDeleting = isDeletingTag === tag.id;
                  const showConfirm = showDeleteConfirm === tag.id;
                  
                  return (
                    <div key={tag.id} className='relative'>
                      <Badge
                        variant={getTagVariant(tag)}
                        style={{ 
                          backgroundColor: isSelected ? tag.color : "transparent", 
                          borderColor: tag.color, 
                          color: isSelected ? "white" : tag.color,
                          borderWidth: '2px'
                        }}
                        className={`cursor-pointer hover:opacity-80 active:scale-95 transition-all duration-200 border-2 h-12 text-sm min-h-[48px] min-w-[60px] px-4 flex items-center justify-center touch-manipulation select-none ${showConfirm ? 'opacity-50' : ''} ${longPressedTag === tag.id ? 'scale-95' : ''}`}
                        onClick={() => handleTagClick(tag)}
                        onTouchStart={isCustomTag ? () => handleLongPressStart(tag.id) : undefined}
                        onTouchEnd={isCustomTag ? handleLongPressEnd : undefined}
                        onMouseDown={isCustomTag ? () => handleLongPressStart(tag.id) : undefined}
                        onMouseUp={isCustomTag ? handleLongPressEnd : undefined}
                        onMouseLeave={isCustomTag ? handleLongPressEnd : undefined}
                        role="button"
                        tabIndex={0}
                        aria-label={`${tag.name} 태그 ${isSelected ? '선택됨' : '선택 안됨'}${isCustomTag ? ', 길게 눌러서 삭제' : ''}`}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handleTagClick(tag);
                          } else if (e.key === 'Delete' && isCustomTag) {
                            e.preventDefault();
                            setShowDeleteConfirm(tag.id);
                          }
                        }}
                      >
                        <span className="text-center">{tag.name}</span>
                        {isCustomTag && (
                          <span className='ml-2 text-xs opacity-75' aria-hidden="true">⭐</span>
                        )}
                      </Badge>
                      
                      {/* 삭제 확인 오버레이 */}
                      {showConfirm && (
                        <div className="absolute inset-0 flex items-center justify-center bg-white/95 dark:bg-gray-900/95 rounded-md border-2 border-red-500 z-10">
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              variant="destructive"
                              className="h-8 px-2 text-xs"
                              onClick={() => {
                                const tagToDelete = allTags.find(t => t.id === tag.id);
                                if (tagToDelete) {
                                  deleteCustomTag(tag.id, tagToDelete.name);
                                }
                              }}
                              disabled={isDeleting}
                              aria-label={`${tag.name} 태그 삭제 확인`}
                            >
                              {isDeleting ? "..." : <Trash2Icon className="w-3 h-3" />}
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-8 px-2 text-xs"
                              onClick={() => setShowDeleteConfirm(null)}
                              disabled={isDeleting}
                              aria-label="삭제 취소"
                            >
                              ✕
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className='space-y-4 pt-6 border-t-2 border-dashed'>
        <Label className='text-sm font-medium flex items-center gap-2'>
          <PlusIcon className='w-4 h-4' />
          새로운 감정 태그 만들기
        </Label>
        {isAddingTag ? (
          <div className='space-y-4 p-4 bg-muted/40 rounded-xl border-2 border-dashed'>
            <div className='space-y-3'>
              <div className='flex flex-col sm:flex-row gap-3'>
                <Input 
                  value={newTagName} 
                  onChange={e => setNewTagName(e.target.value)} 
                  placeholder='감정 이름 입력...' 
                  onKeyDown={e => { 
                    if (e.key === "Enter") { 
                      createCustomTag(); 
                    } else if (e.key === "Escape") { 
                      setIsAddingTag(false); 
                      setNewTagName(""); 
                      setNewTagCategory("neutral"); 
                    } 
                  }} 
                  className='flex-1 h-12 text-sm touch-manipulation'
                  maxLength={20}
                  aria-label="새 감정 태그 이름"
                />
                <Select 
                  value={newTagCategory} 
                  onValueChange={(value: "positive" | "negative" | "neutral") => setNewTagCategory(value)}
                >
                  <SelectTrigger className='w-full sm:w-36 h-12 touch-manipulation'>
                    <div className="flex items-center gap-2">
                      {getCategoryIcon(newTagCategory)}
                      <SelectValue />
                    </div>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value='positive' className="flex items-center gap-2">
                      <div className="flex items-center gap-2">
                        <HeartIcon className="w-4 h-4 text-green-600" />
                        긍정적
                      </div>
                    </SelectItem>
                    <SelectItem value='negative'>
                      <div className="flex items-center gap-2">
                        <FrownIcon className="w-4 h-4 text-red-600" />
                        부정적
                      </div>
                    </SelectItem>
                    <SelectItem value='neutral'>
                      <div className="flex items-center gap-2">
                        <MehIcon className="w-4 h-4 text-gray-600" />
                        중립적
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {newTagName.trim() && (
                <div className='p-3 bg-muted/60 rounded-lg border'>
                  <div className='text-xs text-muted-foreground mb-2'>미리보기:</div>
                  <Badge
                    style={{ 
                      backgroundColor: "transparent", 
                      borderColor: newTagCategory === 'positive' ? '#10B981' : newTagCategory === 'negative' ? '#EF4444' : '#6B7280',
                      color: newTagCategory === 'positive' ? '#10B981' : newTagCategory === 'negative' ? '#EF4444' : '#6B7280',
                      borderWidth: '2px'
                    }}
                    className='border-2 h-12 text-sm min-w-[60px] px-4 flex items-center justify-center'
                  >
                    {newTagName.trim()}
                    <span className='ml-2 text-xs opacity-75'>⭐</span>
                  </Badge>
                </div>
              )}
            </div>
            <div className='flex flex-col sm:flex-row gap-3'>
              <Button 
                onClick={createCustomTag} 
                disabled={!newTagName.trim() || isCreatingTag}
                className='flex-1 h-12 touch-manipulation'
                aria-label="새 감정 태그 생성"
              >
                {isCreatingTag ? (
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    생성 중...
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <PlusIcon className='w-4 h-4' />
                    태그 추가
                  </div>
                )}
              </Button>
              <Button 
                variant='outline' 
                onClick={() => { 
                  setIsAddingTag(false); 
                  setNewTagName(""); 
                  setNewTagCategory("neutral"); 
                }} 
                disabled={isCreatingTag}
                className='flex-1 h-12 touch-manipulation'
                aria-label="새 태그 만들기 취소"
              >
                취소
              </Button>
            </div>
          </div>
        ) : (
          <Button 
            variant='outline' 
            onClick={() => setIsAddingTag(true)} 
            className='w-full h-12 touch-manipulation border-2 border-dashed hover:border-solid'
            aria-label="새 감정 태그 만들기 시작"
          >
            <PlusIcon className='w-5 h-5 mr-2' />
            나만의 감정 태그 만들기
          </Button>
        )}
      </div>
    </div>
  );
}
