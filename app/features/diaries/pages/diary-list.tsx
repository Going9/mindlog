import { useMemo } from "react";
import { Link, useNavigation } from "react-router";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "~/common/components/ui/breadcrumb";
import { DiaryListSkeleton } from "~/common/components/ui/loading";
import { getEmotionTags } from "../../emotions/queries";
import { DiaryCard } from "../components/diary-card";
import { DiaryFilters } from "../components/diary-filters";
import { DiaryListHeader } from "../components/diary-list-header";
import { DiaryPagination } from "../components/diary-pagination";
import { DiarySidebar } from "../components/diary-sidebar";
import { EmptyState } from "../components/empty-state";
import { useDiaryActions } from "../hooks/useDiaryActions";
import { useDiaryFilters } from "../hooks/useDiaryFilters";
import { usePagination } from "../hooks/usePagination";
import { getDiaries, getDiaryDatesForCalendar } from "../queries";
import type { DiaryEntry } from "../types/diary";
import type { Route } from "./+types/diary-list";
import { diaries } from "../schema";

export const loader = async ({ request }: { request: Request }) => {
  const profileId = "b0e0e902-3488-4c10-9621-fffde048923c";
  const url = new URL(request.url);

  // 서버사이드 파라미터 파싱
  const searchQuery = url.searchParams.get("search") || undefined;
  const sortBy = (url.searchParams.get("sortBy") as any) || "date-desc";
  const emotionTagId = url.searchParams.get("emotionTagId")
    ? parseInt(url.searchParams.get("emotionTagId")!)
    : undefined;
  const page = parseInt(url.searchParams.get("page") || "1");
  const limit = 20; // 페이지당 20개
  // 1페이지: (1 - 1) * 20 = 0 → 0개를 건너뛰고 1번 아이템부터 보여줍니다.
  // 2페이지: (2 - 1) * 20 = 20 → 20개를 건너뛰고 21번 아이템부터 보여줍니다.
  // 3페이지: (3 - 1) * 20 = 40 → 40개를 건너뛰고 41번 아이템부터 보여줍니다.
  const offset = (page - 1) * limit;

  // 날짜 필터는 서버사이드로 처리
  const date = url.searchParams.get("date")
    ? new Date(url.searchParams.get("date")!)
    : undefined;

  const currentYear = new Date().getFullYear();

  // Promise.all은 동시에 실행시키고 모두 완료될 때 까지 대기
  const [diaries, emotionTags, calendarDates] = await Promise.all([
    getDiaries({
      profileId,
      searchQuery,
      sortBy,
      emotionTagId,
      date,
      limit,
      offset,
    }),
    getEmotionTags(profileId),
    // 캘린더용 날짜 데이터 (현재 년도, 매우 빠른 쿼리)
    getDiaryDatesForCalendar(profileId, currentYear),
  ]);

  return {
    diaries,
    emotionTags,
    calendarDates, // 캘린더용 날짜 배열
    pagination: {
      currentPage: page,
      limit,
      hasNextPage: diaries.length === limit, // 정확한 개수면 다음 페이지 있을 가능성
    },
    filters: {
      searchQuery,
      sortBy,
      emotionTagId,
      date: date?.toISOString(),
    },
  };
};

export default function DiaryListPage({ loaderData }: Route.ComponentProps) {
  const { diaries, emotionTags, calendarDates, pagination, filters } =
    loaderData;
  const navigation = useNavigation();
  const isLoading = navigation.state === "loading";

  // Custom hooks for state management
  const [filterState, filterActions] = useDiaryFilters(filters, emotionTags);
  const { handleEdit, handleDelete, handleView } = useDiaryActions();
  const { goToPreviousPage, goToNextPage, canGoToPrevious, canGoToNext } =
    usePagination(pagination);

  // 서버사이드 필터링된 데이터에 클라이언트사이드 필터링 적용 (완료도만)
  const filteredEntries = useMemo(() => {
    // 서버에서 이미 검색, 정렬, 감정태그, 날짜 필터링이 적용됨
    // 클라이언트에서는 완료도 필터만 추가 적용
    let filtered: DiaryEntry[] = diaries;

    if (filterState.completionFilter === "complete") {
      filtered = filtered.filter(
        (entry: DiaryEntry) => entry.completedSteps === entry.totalSteps
      );
    } else if (filterState.completionFilter === "incomplete") {
      filtered = filtered.filter(
        (entry: DiaryEntry) => entry.completedSteps < entry.totalSteps
      );
    }

    return filtered;
  }, [diaries, filterState.completionFilter]);

  return (
    <div className='container mx-auto max-w-7xl px-4 py-8'>
      {/* Breadcrumb */}
      <Breadcrumb className='mb-6'>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link to='/'>홈</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>일기 목록</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header */}
      <DiaryListHeader 
        totalDiaries={diaries.length} 
        profileId="b0e0e902-3488-4c10-9621-fffde048923c"
      />

      {/* Main Content */}
      <div className='grid grid-cols-1 lg:grid-cols-4 gap-8'>
        {/* Calendar Sidebar */}
        <DiarySidebar
          calendarDates={calendarDates} // 날짜 배열 사용
          selectedDate={filterState.selectedDate}
          onDateSelect={filterActions.handleDateSelect}
          profileId="b0e0e902-3488-4c10-9621-fffde048923c"
        />

        {/* Entries Section */}
        <div className='lg:col-span-3 space-y-6'>
          {/* Filters */}
          <DiaryFilters
            searchQuery={filterState.searchQuery}
            onSearchChange={filterActions.handleSearchChange}
            sortBy={filterState.sortBy}
            onSortChange={filterActions.handleSortChange}
            selectedEmotionFilter={filterState.selectedEmotionFilter}
            onEmotionFilterChange={filterActions.handleEmotionFilterChange}
            completionFilter={filterState.completionFilter}
            onCompletionFilterChange={
              filterActions.handleCompletionFilterChange
            }
            availableEmotions={emotionTags}
            onClearFilters={filterActions.clearFilters}
            totalEntries={diaries.length}
            filteredEntries={filteredEntries.length}
            isLoading={isLoading}
          />

          {/* Entries Grid */}
          {isLoading ? (
            <DiaryListSkeleton count={6} />
          ) : filteredEntries.length === 0 ? (
            <EmptyState
              type={
                filterState.selectedDate
                  ? "date-no-entries"
                  : diaries.length === 0
                    ? "no-entries"
                    : "no-results"
              }
              selectedDate={filterState.selectedDate}
              onClearFilters={filterActions.clearFilters}
            />
          ) : (
            <>
              <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6'>
                {filteredEntries.map((entry: DiaryEntry) => (
                  <DiaryCard
                    key={entry.id}
                    entry={{ ...entry, date: new Date(entry.date) }}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                    onView={handleView}
                  />
                ))}
              </div>

              {/* Pagination */}
              <DiaryPagination
                currentPage={pagination.currentPage}
                canGoToPrevious={canGoToPrevious}
                canGoToNext={canGoToNext}
                onPreviousPage={goToPreviousPage}
                onNextPage={goToNextPage}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
