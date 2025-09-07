import { useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router";
import type { EmotionTag } from "../types/diary";

// export interface EmotionTag {
//   id: number;
//   name: string;
//   color: string | null;
//   category: "positive" | "negative" | "neutral" | null;
//   isDefault: boolean | null;
//   usageCount?: number | null;
// }

type EmotionTagType = EmotionTag;

type FilterState = {
  searchQuery: string;
  sortBy: string;
  selectedEmotionFilter?: EmotionTagType;
  completionFilter: string;
  selectedDate?: Date;
};

type FilterActions = {
  handleSearchChange: (query: string) => void;
  handleSortChange: (sort: string) => void;
  handleEmotionFilterChange: (emotion?: EmotionTagType) => void;
  handleCompletionFilterChange: (filter: string) => void;
  handleDateSelect: (date: Date | undefined) => void;
  clearFilters: () => void;
};

export function useDiaryFilters(
  initialFilters: {
    searchQuery?: string;
    sortBy?: string;
    emotionTagId?: number;
    date?: string;
  },
  emotionTags: EmotionTagType[]
): [FilterState, FilterActions] {
  const navigate = useNavigate();

  const [searchQuery, setSearchQuery] = useState(
    initialFilters.searchQuery || ""
  );
  const [sortBy, setSortBy] = useState(initialFilters.sortBy || "date-desc");
  const [selectedEmotionFilter, setSelectedEmotionFilter] = useState<
    EmotionTagType | undefined
  >(
    initialFilters.emotionTagId
      ? emotionTags.find(tag => tag.id === initialFilters.emotionTagId)
      : undefined
  );
  const [completionFilter, setCompletionFilter] = useState("all");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(
    initialFilters.date ? new Date(initialFilters.date) : undefined
  );

  // URL navigation helper
  const updateUrlParams = useCallback(
    (newParams: Record<string, string | undefined>) => {
      const url = new URL(window.location.href);

      Object.entries(newParams).forEach(([key, value]) => {
        if (value) {
          url.searchParams.set(key, value);
        } else {
          url.searchParams.delete(key);
        }
      });

      if (Object.keys(newParams).some(key => key !== "page")) {
        url.searchParams.set("page", "1");
      }

      navigate(url.pathname + url.search, { replace: true });
    },
    [navigate]
  );

  const handleSearchChange = useCallback(
    (query: string) => {
      setSearchQuery(query);
      updateUrlParams({ search: query || undefined });
    },
    [updateUrlParams]
  );

  const handleSortChange = useCallback(
    (sort: string) => {
      setSortBy(sort);
      updateUrlParams({ sortBy: sort });
    },
    [updateUrlParams]
  );

  const handleEmotionFilterChange = useCallback(
    (emotion?: EmotionTagType) => {
      setSelectedEmotionFilter(emotion);
      updateUrlParams({ emotionTagId: emotion?.id.toString() });
    },
    [updateUrlParams]
  );

  const handleCompletionFilterChange = useCallback((filter: string) => {
    setCompletionFilter(filter);
  }, []);

  const handleDateSelect = useCallback(
    (date: Date | undefined) => {
      setSelectedDate(date);
      updateUrlParams({
        date: date ? 
          `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` 
          : undefined,
      });
    },
    [updateUrlParams]
  );

  const clearFilters = useCallback(() => {
    setSearchQuery("");
    setSortBy("date-desc");
    setSelectedEmotionFilter(undefined);
    setCompletionFilter("all");
    setSelectedDate(undefined);

    navigate(window.location.pathname, { replace: true });
  }, [navigate]);

  const filterState = useMemo(
    () => ({
      searchQuery,
      sortBy,
      selectedEmotionFilter,
      completionFilter,
      selectedDate,
    }),
    [searchQuery, sortBy, selectedEmotionFilter, completionFilter, selectedDate]
  );

  const filterActions = useMemo(
    () => ({
      handleSearchChange,
      handleSortChange,
      handleEmotionFilterChange,
      handleCompletionFilterChange: handleCompletionFilterChange,
      handleDateSelect,
      clearFilters,
    }),
    [
      handleSearchChange,
      handleSortChange,
      handleEmotionFilterChange,
      handleCompletionFilterChange,
      handleDateSelect,
      clearFilters,
    ]
  );

  return [filterState, filterActions];
}
