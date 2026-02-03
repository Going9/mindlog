# 2026-02-03 Preline UI 전역 에러 수정 (TypeError: Cannot read properties of undefined (reading 'length'))

## 1. 문제 상황
- 일기 쓰기 페이지(`diaries/form.html`) 및 기타 페이지에서 다음과 같은 에러 발생:
  `preline.js:96 Uncaught TypeError: Cannot read properties of undefined (reading 'length')`
- 에러 원인: Preline 2.7.0 라이브러리가 로드될 때 `window.addEventListener("resize", ...)`와 같은 전역 이벤트 리스너를 등록함. 이 리스너 내부에서 `$hsOverlayCollection`, `$hsComboBoxCollection` 등 Preline 컴포넌트 컬렉션을 참조하는데, 해당 컴포넌트가 페이지에 없어 초기화되지 않은 경우(undefined) `.length` 속성에 접근하려다 에러가 발생함.

## 2. 해결 방법
- 모든 Preline 컴포넌트 컬렉션이 `preline.js`가 로드되기 전에 최소한 빈 배열(`[]`)로 초기화되어 있도록 보장함.
- `turbo-init.js` 파일의 최상단에 Preline이 사용하는 25개의 컬렉션 이름을 가진 전역 변수를 선언하고, 이미 존재하지 않는 경우 빈 배열을 할당하는 로직을 추가함.
- `turbo-init.js`는 `head.html`에서 `preline.js`보다 먼저 로드되므로, 모든 페이지에서 안전하게 에러를 방지할 수 있음.

## 3. 변경 사항
- `src/main/resources/static/js/turbo-init.js`: Preline 컬렉션 전역 초기화 코드 추가.

## 4. 결과 확인
- `./gradlew build -x test` 실행 결과 정상.
- 전역 이벤트 리스너(resize 등)가 발생해도 컬렉션이 정의되어 있으므로 `.length` 참조 에러가 발생하지 않음.
- 일기 쓰기 페이지를 포함한 모든 페이지에서 Preline 관련 JS 에러가 사라짐.
