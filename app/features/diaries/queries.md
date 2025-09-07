● Explain: app/features/diaries/queries.ts

이 파일은 일기 기능의 데이터베이스 연동 로직을 담당하는 핵심 모듈입니다. Supabase를 통해 PostgreSQL 데이터베이스와 통신하는 모든 함수들이 정의되어 있습니다.

📋 주요 역할

- 일기 조회: 목록, 단일 일기, 캘린더용 날짜 조회
- 일기 생성/수정: 새 일기 작성 및 기존 일기 업데이트
- 감정 태그 관리: 일기와 감정 태그 연결
- 데이터 변환: DB 데이터를 프론트엔드에서 사용할 형태로 변환

🗄️ 데이터베이스 구조

diaries emotion_tags diary_tags
┌─────────────┐ ┌──────────────┐ ┌──────────────┐
│ id │ │ id │ │ diary_id │
│ profile_id │ │ name │ │ emotion_tag_id│
│ date │ │ color │ └──────────────┘
│ short_content│ │ category │ │
│ situation │ │ is_default │ │
│ ... │ └──────────────┘ │
└─────────────┘ │ │
│ └──────────────────┘
└─────────── 다대다 관계 (중간테이블)

🔧 주요 함수들

1. getDiaries() - 일기 목록 조회

// 사용 예시
const diaries = await getDiaries({
profileId: "user123",
limit: 20,
offset: 0,
sortBy: "date-desc",
searchQuery: "행복",
date: new Date("2025-09-06"),
emotionTagId: 5
});

실행 흐름:

1. 감정 태그 필터 분기: emotionTagId가 있으면 JOIN 쿼리, 없으면 VIEW 사용
2. 필터 적용: 검색어, 날짜, 감정 태그로 필터링
3. 정렬 및 페이징: sortBy와 limit/offset으로 결과 제한
4. 감정 태그 조회: 각 일기의 모든 감정 태그를 별도 쿼리로 가져옴
5. 데이터 변환: DB 컬럼명을 camelCase로 변환하고 완성도 계산

6. createDiary() - 일기 생성 (Upsert 방식)

// 사용 예시
const newDiary = await createDiary({
profileId: "user123",
date: new Date(),
shortContent: "오늘은 행복한 하루였다",
situation: "친구들과 만났다",
emotionTags: [
{ id: 1, name: "기쁨", color: "#ff0000", category: "positive", isDefault: true }
]
});

실행 흐름:

1. 중복 일기 확인: 같은 날짜에 일기가 있는지 체크
2. 분기 처리:


    - 기존 일기 있음 → 업데이트 + 기존 감정태그 삭제
    - 기존 일기 없음 → 새로 생성

3. 감정 태그 검증: 유효한 태그 ID만 필터링
4. 태그 연결: diary_tags 테이블에 관계 생성
5. 결과 반환: getDiaryById()로 완전한 일기 데이터 반환

6. getDiaryById() - 단일 일기 조회

// 사용 예시
const diary = await getDiaryById(123, "user123");

실행 흐름:

1. 일기 기본정보 조회
2. 감정 태그들 조회 (JOIN으로 연결된 모든 태그)
3. 데이터 변환 및 완성도 계산
4. 통합 객체 반환

5. calculateCompletedSteps() - 완성도 계산

// 내부적으로 사용되는 헬퍼 함수
const completedSteps = calculateCompletedSteps({
short_content: "내용 있음",
situation: null, // 비어있음
reaction: "반응 있음",
// ... 7개 필드 중 비어있지 않은 필드 개수 반환
});
// 결과: 2 (7개 중 2개 완료)

🚀 실제 사용 사례

일기 목록 페이지에서:

// diary-list.tsx의 loader에서
export const loader = async ({ request }) => {
const url = new URL(request.url);
const searchQuery = url.searchParams.get("search");

    const diaries = await getDiaries({
      profileId: getCurrentUserId(),
      searchQuery,
      limit: 20,
      sortBy: "date-desc"
    });

    return { diaries };

};

일기 작성 페이지에서:

// new-diary.tsx에서
const handleSubmit = async (formData) => {
const savedDiary = await createDiary({
profileId: getCurrentUserId(),
date: formData.date,
shortContent: formData.shortContent,
emotionTags: formData.selectedTags
});

    navigate(`/diary/${savedDiary.id}`);

};

🔍 핵심 특징

1. 타입 안전성

- TypeScript로 모든 함수와 데이터 구조 타입 정의
- 컴파일 타임에 오류 방지

2. 에러 처리

- Supabase 에러를 상위로 전파
- 호출하는 곳에서 try-catch로 처리

3. 데이터 정합성

- 감정 태그 유효성 검증
- 중복 일기 방지 (같은 날짜 upsert)

4. 성능 최적화

- Promise.all로 병렬 쿼리
- 필요한 데이터만 select
- 인덱스 활용 (profile_id, date)

이 파일은 일기 기능의 데이터 계층을 담당하며, UI 컴포넌트와 데이터베이스 사이의 다리 역할을 합니다!
