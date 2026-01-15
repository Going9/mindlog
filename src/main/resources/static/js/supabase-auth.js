/**
 * Supabase Auth 유틸리티
 * API 요청 시 JWT 토큰을 자동으로 헤더에 포함합니다.
 */

// Supabase 클라이언트 초기화 (Thymeleaf에서 주입된 변수 사용)
let supabaseClient = null;

/**
 * Supabase 클라이언트 초기화
 * @param {string} url - Supabase URL
 * @param {string} anonKey - Supabase Anon Key
 */
function initSupabase(url, anonKey) {
    if (typeof supabase !== 'undefined') {
        supabaseClient = supabase.createClient(url, anonKey);
    } else {
        console.error('Supabase SDK가 로드되지 않았습니다.');
    }
    return supabaseClient;
}

/**
 * 현재 저장된 액세스 토큰 반환
 * @returns {string|null}
 */
function getAccessToken() {
    return localStorage.getItem('supabase_access_token');
}

/**
 * 현재 저장된 리프레시 토큰 반환
 * @returns {string|null}
 */
function getRefreshToken() {
    return localStorage.getItem('supabase_refresh_token');
}

/**
 * 토큰 저장
 * @param {string} accessToken
 * @param {string} refreshToken
 */
function saveTokens(accessToken, refreshToken) {
    localStorage.setItem('supabase_access_token', accessToken);
    if (refreshToken) {
        localStorage.setItem('supabase_refresh_token', refreshToken);
    }
}

/**
 * 토큰 삭제
 */
function clearTokens() {
    localStorage.removeItem('supabase_access_token');
    localStorage.removeItem('supabase_refresh_token');
}

/**
 * 로그인 여부 확인
 * @returns {boolean}
 */
function isAuthenticated() {
    return !!getAccessToken();
}

/**
 * 인증된 API 요청 (fetch 래퍼)
 * @param {string} url - API URL
 * @param {object} options - fetch 옵션
 * @returns {Promise<Response>}
 */
async function authFetch(url, options = {}) {
    const token = getAccessToken();
    
    if (!token) {
        // 토큰이 없으면 로그인 페이지로 리다이렉트
        window.location.href = '/auth/login';
        throw new Error('인증이 필요합니다.');
    }
    
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };
    
    const response = await fetch(url, {
        ...options,
        headers
    });
    
    // 401 Unauthorized 시 토큰 갱신 시도
    if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            // 갱신 성공 시 재시도
            headers['Authorization'] = `Bearer ${getAccessToken()}`;
            return fetch(url, { ...options, headers });
        } else {
            // 갱신 실패 시 로그인 페이지로
            clearTokens();
            window.location.href = '/auth/login';
            throw new Error('세션이 만료되었습니다.');
        }
    }
    
    return response;
}

/**
 * 액세스 토큰 갱신
 * @returns {Promise<boolean>}
 */
async function refreshAccessToken() {
    if (!supabaseClient) {
        console.error('Supabase 클라이언트가 초기화되지 않았습니다.');
        return false;
    }
    
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        return false;
    }
    
    try {
        const { data, error } = await supabaseClient.auth.refreshSession({
            refresh_token: refreshToken
        });
        
        if (error || !data.session) {
            return false;
        }
        
        saveTokens(data.session.access_token, data.session.refresh_token);
        return true;
    } catch (err) {
        console.error('토큰 갱신 실패:', err);
        return false;
    }
}

/**
 * 로그아웃
 */
async function logout() {
    if (supabaseClient) {
        await supabaseClient.auth.signOut();
    }
    clearTokens();
    window.location.href = '/auth/login';
}

/**
 * 현재 사용자 정보 조회
 * @returns {Promise<object|null>}
 */
async function getCurrentUser() {
    if (!supabaseClient) {
        return null;
    }
    
    try {
        const { data: { user }, error } = await supabaseClient.auth.getUser();
        if (error) {
            return null;
        }
        return user;
    } catch (err) {
        console.error('사용자 정보 조회 실패:', err);
        return null;
    }
}

/**
 * 페이지 로드 시 인증 상태 확인
 * 인증이 필요한 페이지에서 호출
 */
function requireAuth() {
    if (!isAuthenticated()) {
        window.location.href = '/auth/login';
    }
}

// 전역 객체로 내보내기
window.MindlogAuth = {
    initSupabase,
    getAccessToken,
    getRefreshToken,
    saveTokens,
    clearTokens,
    isAuthenticated,
    authFetch,
    refreshAccessToken,
    logout,
    getCurrentUser,
    requireAuth
};
