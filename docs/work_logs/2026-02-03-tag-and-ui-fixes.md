# 2026-02-03 태그, UI 및 Turbo Frame 수정 작업 내역

## 1. 태그 생성 및 수정 오류 해결
- **문제**: 일기 수정 화면에서 태그 추가 모달이 동작하지 않음 (`ReferenceError: openModal is not defined`).
- **원인**: 모달 HTML과 관련 스크립트가 `<turbo-frame>` 외부에 위치하여, 프레임 갱신 시(상세 -> 수정) 로드되지 않음.
- **해결**: `diaries/form.html` 내의 태그 모달(`id="tag-modal"`)과 스크립트를 `<turbo-frame id="diary_context">` 내부로 이동.

## 2. Turbo Frame 네비게이션 오류 해결
- **문제**: 일기 저장 후 "The response ... did not contain the expected <turbo-frame>" 에러 발생.
- **원인**: 폼 제출 후 리다이렉트되는 페이지(목록/상세)에 폼과 동일한 ID의 프레임이 없거나 전체 페이지 로드가 필요함.
- **해결**: `<form>` 태그에 `data-turbo-frame="_top"` 속성을 추가하여 제출 시 전체 페이지 네비게이션을 강제함.

## 3. 에러 핸들링 표준화
- **서버 사이드**: `GlobalExceptionHandler`를 추가하여 태그 중복(`IllegalArgumentException`) 시 500 에러 대신 **422 Unprocessable Entity**를 반환하도록 수정.
- **클라이언트 사이드**: `createTag` 함수에서 `alert()` 대신 모달 내부에 에러 메시지(`div#tag-error-message`)를 인라인으로 표시하도록 개선 (Turbo 표준 UX 지향).

## 4. 날짜 입력 포맷 수정
- **문제**: 수정 화면 진입 시 날짜 필드가 비어있거나 "The specified value ... does not conform" 에러 발생.
- **원인**: `th:field`가 로케일 포맷(예: 26. 2. 3.)으로 값을 렌더링하여 HTML5 `input type="date"`의 요구사항(`yyyy-MM-dd`)과 불일치.
- **해결**: `th:field`를 제거하고 `name="date"`, `id="date"`, `th:value="${#temporals.format(..., 'yyyy-MM-dd')}"`를 명시적으로 사용하여 ISO 포맷 강제.

## 5. 사용자 이름 표시
- **문제**: 네비게이션 바에 사용자 이름 대신 UUID가 표시됨.
- **해결**: `AuthLoginService`에서 로그인 시 Supabase 메타데이터로부터 이름(`display_name`)을 추출하여 세션(`USER_NAME`)에 저장. `navbar.html`에서 이를 출력하도록 수정.

## 6. 기타 수정
- **JS SyntaxError**: `PRESET_COLORS` 재선언 오류 해결을 위해 `const`를 `var`로 변경.
