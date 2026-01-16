/**
 * Hotwire Turbo 초기화 스크립트
 * 
 * Supabase 인증과 Turbo를 통합하여:
 * - 모든 Turbo 요청에 JWT 토큰을 자동으로 추가
 * - 인증 상태 변경 시 자동 리다이렉트
 */

(function () {
    'use strict';

    // Supabase 클라이언트 초기화 (전역에서 사용 가능하도록)
    let supabaseClient = null;

    /**
     * Supabase 클라이언트 설정
     * @param {string} url - Supabase URL
     * @param {string} anonKey - Supabase Anon Key
     */
    window.initSupabaseForTurbo = function (url, anonKey) {
        if (window.supabaseClient || window.isSupabaseInitializing) return;

        window.isSupabaseInitializing = true;

        function ensureClient() {
            if (typeof window.supabase !== 'undefined' && window.supabase.createClient) {
                if (!window.supabaseClient) {
                    window.supabaseClient = window.supabase.createClient(url, anonKey);
                }
                window.isSupabaseInitializing = false;
            } else {
                setTimeout(ensureClient, 50);
            }
        }
        ensureClient();
    };

    /**
     * 현재 세션의 액세스 토큰 가져오기
     * @returns {Promise<string|null>}
     */
    async function getAccessToken() {
        // 전역 클라이언트 우선 사용
        const client = window.supabaseClient || supabaseClient;

        if (!client) {
            // localStorage에서 직접 가져오기 (fallback)
            return localStorage.getItem('supabase_access_token');
        }

        try {
            const { data: { session } } = await client.auth.getSession();
            if (session && session.access_token) {
                return session.access_token;
            }
        } catch (error) {
            console.warn('Failed to get session:', error);
        }

        // Fallback: localStorage에서 가져오기
        return localStorage.getItem('supabase_access_token');
    }

    /**
     * Turbo 요청에 Authorization 헤더 추가
     */
    document.addEventListener('turbo:before-fetch-request', async function (event) {
        const token = await getAccessToken();

        if (token) {
            // Turbo fetch 요청에 헤더 추가
            event.detail.fetchOptions.headers = event.detail.fetchOptions.headers || {};
            event.detail.fetchOptions.headers['Authorization'] = `Bearer ${token}`;
        }
    });

    /**
     * 인증 실패 시 처리 (401 Unauthorized)
     */
    document.addEventListener('turbo:before-fetch-response', async function (event) {
        const response = event.detail.fetchResponse;

        if (response && response.status === 401) {
            // 인증 실패 시 로그인 페이지로 리다이렉트
            if (window.location.pathname !== '/auth/login') {
                window.location.href = '/auth/login';
            }
        }
    });

    /**
     * Turbo 페이지 로드 후 인증 상태 확인
     */
    document.addEventListener('turbo:load', async function () {
        if (!supabaseClient) return;

        try {
            const { data: { session } } = await supabaseClient.auth.getSession();

            // 세션이 없고 보호된 페이지에 있으면 로그인 페이지로 리다이렉트
            if (!session && !isPublicPage()) {
                // 무한 리다이렉트 방지
                if (window.location.pathname !== '/auth/login') {
                    window.location.href = '/auth/login';
                }
            }
        } catch (error) {
            console.warn('Auth check failed:', error);
        }
    });

    /**
     * 쿠키 설정
     */
    function setCookie(name, value, seconds) {
        let expires = "";
        if (seconds) {
            const date = new Date();
            date.setTime(date.getTime() + (seconds * 1000));
            expires = "; expires=" + date.toUTCString();
        }
        document.cookie = name + "=" + (value || "") + expires + "; path=/; SameSite=Lax";
    }

    /**
     * 쿠키 삭제
     */
    function eraseCookie(name) {
        document.cookie = name + '=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    }

    /**
     * 공개 페이지인지 확인
     * @returns {boolean}
     */
    function isPublicPage() {
        const publicPaths = ['/', '/auth/login', '/auth/callback', '/auth/logout'];
        return publicPaths.includes(window.location.pathname);
    }

    /**
     * Supabase 인증 상태 변경 감지 및 쿠키 동기화
     */
    function initAuthListeners() {
        const client = window.supabaseClient;
        if (!client) {
            setTimeout(initAuthListeners, 100);
            return;
        }

        // 초기 세션 확인 및 쿠키 설정
        client.auth.getSession().then(({ data: { session } }) => {
            if (session) {
                setCookie('access_token', session.access_token, 3600);
            }
        });

        client.auth.onAuthStateChange((event, session) => {
            if (session) {
                // 세션이 있으면 쿠키에 토큰 저장 (액세스 토큰 만료 시간과 동기화하거나 적절한 시간 설정)
                setCookie('access_token', session.access_token, 3600);
            } else if (event === 'SIGNED_OUT') {
                // 로그아웃 시 쿠키 삭제
                eraseCookie('access_token');
            }

            if (event === 'SIGNED_OUT' && !isPublicPage()) {
                window.location.href = '/auth/login';
            } else if (event === 'SIGNED_IN' && window.location.pathname === '/auth/login') {
                window.location.href = '/';
            }
        });
    }

    // 초기화 시작
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAuthListeners);
    } else {
        initAuthListeners();
    }
})();
