import * as Turbo from "@hotwired/turbo"

// 1. Turbo 시작 (가장 중요)
Turbo.start()

// 2. 디버깅을 위해 전역 변수에 등록
window.Turbo = Turbo

// 3. [수정됨] 수동 연결 로직 삭제
// // // 안드로이드 앱이 실행되면 자동으로 네이티브 어댑터가 연결
// // // 웹 코드에서 connect()를 강제로 호출할 필요 X

// 4. 로드 확인 로그
document.addEventListener("turbo:load", () => {
    console.log("[Mindlog] Turbo 화면 로드 완료")
})

// 5. 인증 만료 처리
// 서버에서 401(Unauthorized)이나 403(Forbidden) 응답이 오면 로그인 페이지로
document.addEventListener('turbo:before-fetch-response', (event) => {
    const response = event.detail.fetchResponse
    const status = response.status

    if ((status === 401 || status === 403) && window.location.pathname !== '/auth/login') {
        event.preventDefault() // Turbo의 에러 처리를 막고
        console.log("[Mindlog] 세션 만료 감지 -> 로그인 페이지 이동")
        window.location.href = '/auth/login?error=session_expired' // 강제 이동
    }
})