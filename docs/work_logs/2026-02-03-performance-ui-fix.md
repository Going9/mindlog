# 2026-02-03 성능 개선 및 UI 수정 작업 내역

## 1. 성능 개선 (Logging & Analysis)
- **문제**: 로그가 불명확하여 병목 지점 파악이 어려움.
- **해결**: `PerformanceLoggingAspect.java` 수정. `log.warn` 메시지에 메서드 이름과 실행 시간(ms)을 명시적으로 포함하도록 변경.
- **분석**: `DiaryService.getMonthlyDiaries`는 이미 Batch Fetch(`findAllByDiaryIdIn`)를 사용하고 있어 N+1 문제는 없음. 로그인 지연은 Supabase 외부 API 호출(약 700ms+)이 주원인으로 파악됨.

## 2. 로그인/로그아웃 개선
- **문제**: 카카오 로그아웃 후 다시 로그인 시 계정 선택 없이 자동 로그인됨.
- **해결**: `AuthController`에서 소셜 로그인 리다이렉트 URL 생성 시 `prompt=select_account` (Google) 및 `prompt=login` (Kakao) 파라미터를 추가하여 강제로 계정 선택/재로그인을 유도하도록 수정.
- **UX**: 로그인 버튼 클릭 시 '이동 중...' 및 스피너를 표시하여 사용자 피드백 강화 (`auth/login.html`).

## 3. UI/UX 개선
- **메인 화면 (`diaries/index.html`)**:
  - 카드 제목이 길어질 경우 레이아웃이 깨지는 문제 해결 (`line-clamp-2` 적용).
  - 카드 높이 통일감을 위해 구조 개선.
- **상세 화면 (`diaries/detail.html`)**:
  - "나에게 해주고 싶은 말" 카드의 검은색 배경을 제거하고 전체 스타일과 통일(흰색 배경, 테두리).
  - 헤더와 콘텐츠 간의 수직 간격(`space-y-6`)을 통일하여 레이아웃 안정화.
- **Preline UI 오류**:
  - Navbar 햄버거 메뉴 동작 문제 해결을 위해 `turbo-init.js`에서 `turbo:load` 이벤트 발생 시 `HSStaticMethods.autoInit()`을 호출하도록 추가.

## 4. 버그 수정
- **태그 생성 오류**: 태그 중복 시 500 에러 대신 422 상태 코드를 반환하도록 `GlobalExceptionHandler` 추가 및 클라이언트 JS 수정.
- **날짜 입력 오류**: 수정 시 날짜가 비어있는 문제를 해결하기 위해 `th:value`로 날짜 포맷(`yyyy-MM-dd`)을 명시적 지정.
- **Preline JS 오류**: `PRESET_COLORS` 변수 재선언 문제 해결 (`const` -> `var`).

## 5. 빌드 설정
- **Gradle**: `npm` 경로 인식 문제를 해결하기 위해 OS별 경로 감지 로직 추가 및 `buildCss` 태스크 등록. `processResources`가 CSS 빌드에 의존하도록 설정.