(function () {
    'use strict';
    if (window.__MINDLOG_INITIALIZED__) return;
    window.__MINDLOG_INITIALIZED__ = true;

    // [수정] 더 이상 JS에서 토큰 유무를 감시하지 않음 (서버 세션 사용)

    // Turbo가 서버로부터 401(인증 없음) 또는 403(권한 없음) 응답을 받았을 때만 처리
    document.addEventListener('turbo:before-fetch-response', (event) => {
        const status = event.detail.fetchResponse.status;
        const path = window.location.pathname;

        if ((status === 401 || status === 403) && path !== '/auth/login' && path !== '/') {
            console.warn('[Mindlog] 인증 만료 또는 권한 없음. 로그인 페이지로 이동합니다.');
            // Turbo 방문이 아니라 완전한 페이지 이동으로 처리하여 세션 정리 유도
            window.location.href = '/auth/login?error=session_expired';
        }
    });

    console.log("[Mindlog] Turbo 초기화 완료 (Server-side Session Mode)");
})();