import * as Turbo from "@hotwired/turbo"

// 1. 전역 변수에 Turbo 등록 (안드로이드가 찾을 수 있게)
window.Turbo = Turbo;

// 2. Turbo 시작
Turbo.start();

// 3. [핵심] 브릿지 수동 연결 (타이밍 이슈 해결)
// 안드로이드 앱 내부에서 실행된 경우에만 작동합니다.
if (window.Turbo.session.adapter) {
    window.Turbo.session.adapter.connect();
    console.log("[Mindlog] Native Bridge Connected Manually");
} else {
    // 만약 어댑터가 아직 없다면, 약간 기다렸다가 다시 시도 (안전장치)
    setTimeout(() => {
        if (window.Turbo.session.adapter) {
            window.Turbo.session.adapter.connect();
            console.log("[Mindlog] Native Bridge Connected (Delayed)");
        }
    }, 100);
}

// 4. 인증 만료 처리 (기존 로직 유지)
if (!window.__MINDLOG_TURBO_INITIALIZED__) {
    window.__MINDLOG_TURBO_INITIALIZED__ = true;
    document.addEventListener('turbo:before-fetch-response', (event) => {
        const status = event.detail.fetchResponse.status;
        if ((status === 401 || status === 403) && window.location.pathname !== '/auth/login') {
            window.location.href = '/auth/login?error=session_expired';
        }
    });
}