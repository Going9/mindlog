(function () {
    'use strict';
    if (window.__MINDLOG_INITIALIZED__) return;
    window.__MINDLOG_INITIALIZED__ = true;

    const COOKIE_NAME = 'mindlog_access_token';

    window.initSupabaseForTurbo = function (url, anonKey) {
        if (window.supabaseClient) return;

        // 라이브러리(SDK)가 로드될 때까지 기다렸다가 생성합니다.
        const tryCreate = () => {
            if (typeof window.supabase !== 'undefined' && window.supabase.createClient) {
                window.supabaseClient = window.supabase.createClient(url, anonKey, {
                    auth: { persistSession: true, autoRefreshToken: true }
                });
                console.log("[Mindlog] Supabase 클라이언트가 생성되었습니다.");
                setupAuthListeners();
            } else {
                // 아직 로드 안 됐으면 50ms 후 다시 시도
                setTimeout(tryCreate, 50);
            }
        };
        tryCreate();
    };

    function setupAuthListeners() {
        const client = window.supabaseClient;
        if (!client || window.__MINDLOG_LISTENERS_SET__) return;
        window.__MINDLOG_LISTENERS_SET__ = true;

        client.auth.onAuthStateChange((event, session) => {
            const token = session?.access_token;
            if (token) {
                document.cookie = `${COOKIE_NAME}=${token}; path=/; max-age=3600; SameSite=Lax`;
                localStorage.setItem('supabase_access_token', token);
                const path = window.location.pathname;
                if (path === '/' || path === '/auth/login' || path === '/auth/callback') {
                    window.location.href = '/diaries';
                }
            } else {
                document.cookie = `${COOKIE_NAME}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
                localStorage.removeItem('supabase_access_token');
                if (!isPublicPage()) window.location.href = '/auth/login';
            }
        });
    }

    function isPublicPage() {
        return ['/', '/auth/login', '/auth/callback', '/auth/logout'].includes(window.location.pathname);
    }

    document.addEventListener('turbo:before-fetch-request', (event) => {
        const token = localStorage.getItem('supabase_access_token');
        if (token) {
            event.detail.fetchOptions.headers['Authorization'] = `Bearer ${token}`;
        }
    });

    document.addEventListener('turbo:before-fetch-response', (event) => {
        if (event.detail.fetchResponse.status === 401 && !isPublicPage()) {
            window.location.href = '/auth/login';
        }
    });
})();