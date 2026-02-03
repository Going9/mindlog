# 2026-02-03 삭제 에러 및 사용자 이름 표시 수정

## 1. 삭제 에러 해결 (Turbo Frame)
- **문제**: 일기 상세 화면에서 삭제 시 "The response ... did not contain the expected <turbo-frame id="diary_context">" 에러 발생.
- **원인**: 삭제 후 리다이렉트되는 목록 페이지에는 상세 화면에 있던 `diary_context` 프레임이 없어 Turbo가 업데이트에 실패함.
- **해결**: `diaries/detail.html`의 삭제 폼에 `data-turbo-frame="_top"` 속성을 추가하여, 삭제 후 전체 페이지 전환이 일어나도록 수정.

## 2. 사용자 이름 표시 개선
- **문제**: 네비게이션 바에 이름 대신 "사용자" 또는 UUID가 표시되는 문제.
- **원인**: 세션에 `USER_NAME`이 없는 경우(기존 세션 등)에 대한 처리 부족.
- **해결**:
    - `GlobalModelAdvice.java`의 `userName` 로직 보완.
    - 세션에 이름이 없더라도 인증된 상태라면 `ProfileRepository`를 통해 DB에서 이름을 직접 조회하도록 로직 추가.
    - 조회된 이름은 다시 세션에 저장하여 성능 최적화(Self-healing).
    - DB 조회 실패 시 최소한 UUID라도 표시되도록 최종 폴백 로직 적용.

## 3. 문서화 규칙 적용
- `AGENTS.md`에 "ulw" 작업 내역 기록 의무화 규칙 추가 및 실천.
