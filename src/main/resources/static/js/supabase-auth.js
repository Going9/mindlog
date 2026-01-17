/**
 * Supabase Auth 유틸리티
 * - 직접 클라이언트를 생성하지 않고 turbo-init에서 생성된 객체를 사용합니다.
 */
(function() {
    'use strict';

    // 전역 인스턴스 가져오기 헬퍼
    const getClient = () => window.supabaseClient;

    /**
     * 인증된 API 요청용 fetch 래퍼
     */
    async function authFetch(url, options = {}) {
        const token = localStorage.getItem('supabase_access_token');
        if (!token) {
            window.location.href = '/auth/login';
            throw new Error('인증 정보가 없습니다.');
        }

        const headers = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            ...options.headers
        };

        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            const refreshed = await window.MindlogAuth.refreshAccessToken();
            if (refreshed) {
                headers['Authorization'] = `Bearer ${localStorage.getItem('supabase_access_token')}`;
                return fetch(url, { ...options, headers });
            } else {
                window.location.href = '/auth/login';
            }
        }
        return response;
    }

    /**
     * 세션 갱신
     */
    async function refreshAccessToken() {
        const client = getClient();
        if (!client) return false;

        try {
            const { data, error } = await client.auth.refreshSession();
            if (error || !data.session) return false;

            localStorage.setItem('supabase_access_token', data.session.access_token);
            return true;
        } catch (e) {
            return false;
        }
    }

    /**
     * 현재 사용자 정보 가져오기
     */
    async function getCurrentUser() {
        const client = getClient();
        if (!client) return null;
        const { data: { user } } = await client.auth.getUser();
        return user;
    }

    /**
     * 로그아웃 실행
     */
    async function logout() {
        const client = getClient();
        if (client) await client.auth.signOut();
        localStorage.removeItem('supabase_access_token');
        window.location.href = '/auth/login';
    }

    // 외부 노출 객체
    window.MindlogAuth = {
        authFetch,
        refreshAccessToken,
        getCurrentUser,
        logout,
        isAuthenticated: () => !!localStorage.getItem('supabase_access_token')
    };
})();