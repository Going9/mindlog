package com.mindlog.domain.auth.controller;

import com.mindlog.domain.auth.service.AuthLoginService;
import com.mindlog.global.security.PkceUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String APP_SOURCE = "app";
    private static final String PKCE_VERIFIER_KEY = "pkce_verifier";
    private static final String LOGIN_SOURCE_KEY = "login_source";

    private final AuthLoginService authLoginService;

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @GetMapping("/login")
    public String loginPage() {
        log.debug("[AUTH] 로그인 페이지 요청");
        return "auth/login";
    }

    @GetMapping("/login/{provider}")
    public String socialLogin(@PathVariable String provider,
            @RequestParam(name = "source", required = false) String source,
            HttpServletRequest request,
            HttpSession session) {
        log.debug("[AUTH] ===== 소셜 로그인 시작 =====");
        log.debug("[AUTH] provider={}, source={}", provider, source);
        log.debug("[AUTH] request host/proto - host={}, x-forwarded-host={}, scheme={}, x-forwarded-proto={}",
                request.getHeader("Host"),
                request.getHeader("X-Forwarded-Host"),
                request.getScheme(),
                request.getHeader("X-Forwarded-Proto"));
        log.debug("[AUTH] session - id={}, isNew={}, requestedSessionId={}",
                maskSessionId(session.getId()),
                session.isNew(),
                maskSessionId(request.getRequestedSessionId()));

        if (isAlreadyAuthenticated()) {
            log.debug("[AUTH] 이미 로그인됨 - /로 리다이렉트");
            return "redirect:/";
        }

        var verifier = PkceUtil.generateCodeVerifier();
        var challenge = PkceUtil.generateCodeChallenge(verifier);
        session.setAttribute(PKCE_VERIFIER_KEY, verifier);
        log.debug("[AUTH] PKCE verifier 생성 완료 (길이: {})", verifier.length());

        var isNativeApp = APP_SOURCE.equals(source);
        if (isNativeApp) {
            session.setAttribute(LOGIN_SOURCE_KEY, APP_SOURCE);
            log.debug("[AUTH] 네이티브 앱 요청 - source=app");
        }

        var prompt = "google".equalsIgnoreCase(provider) ? "select_account" : "login";

        var redirectUri = buildRedirectUri(request);
        log.debug("[AUTH] 기본 redirectUri: {}", redirectUri);

        if (isNativeApp) {
            redirectUri = appendNativeSourceAndVerifier(redirectUri, verifier);
            log.debug("[AUTH] 앱용 redirectUri 생성 완료 (verifier 포함)");
        }

        var authUrl = buildAuthUrl(provider, redirectUri, challenge, prompt);

        log.debug("[AUTH] OAuth URL로 리다이렉트");
        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "v", required = false) String encodedVerifier,
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.ui.Model model) {

        log.debug("[AUTH] ===== OAuth 콜백 수신 =====");
        log.debug("[AUTH] code={}, error={}, source={}, v(verifier)={}",
                code != null ? "있음(길이:" + code.length() + ")" : "없음",
                error,
                source,
                encodedVerifier != null ? "있음(길이:" + encodedVerifier.length() + ")" : "없음");
        log.debug("[AUTH] callback request - host={}, scheme={}, sessionCookie={}, requestedSessionId={}, requestedSessionIdValid={}",
                request.getHeader("Host"),
                request.getScheme(),
                maskSessionId(extractSessionCookieValue(request)),
                maskSessionId(request.getRequestedSessionId()),
                request.isRequestedSessionIdValid());

        if (error != null || code == null) {
            log.error("[AUTH] 로그인 실패: code={}, error={}", code, error);
            return buildLoginErrorRedirect("auth_failed", source);
        }

        HttpSession session = request.getSession();
        log.debug("[AUTH] callback session - id={}, isNew={}", maskSessionId(session.getId()), session.isNew());

        // 네이티브 앱 여부 판별 (source 파라미터 또는 User-Agent로 판단)
        var isNativeApp = APP_SOURCE.equals(source) || isNativeAppRequest(request);
        log.debug("[AUTH] 네이티브 앱 여부: {}", isNativeApp);

        String verifier;
        if (encodedVerifier != null && !encodedVerifier.isEmpty()) {
            verifier = new String(Base64.getUrlDecoder().decode(encodedVerifier));
            log.debug("[AUTH] URL 파라미터에서 verifier 추출 성공 (길이: {})", verifier.length());
        } else {
            verifier = (String) session.getAttribute(PKCE_VERIFIER_KEY);
            log.debug("[AUTH] 세션에서 verifier 추출: {}", verifier != null ? "성공" : "실패");
        }

        if (verifier == null) {
            log.error("[AUTH] verifier를 찾을 수 없음 - source={}, encodedVerifier={}", source, encodedVerifier);
            return buildLoginErrorRedirect("invalid_session", isNativeApp ? "app" : source);
        }

        try {
            if (isNativeApp) {
                log.debug("[AUTH] 네이티브 앱 로그인 처리 시작...");
                var handoverToken = authLoginService.processLoginForNativeApp(code, verifier);
                session.removeAttribute(PKCE_VERIFIER_KEY);

                log.info("[AUTH] 네이티브 앱 로그인 성공! handoverToken 생성 완료");

                model.addAttribute("deepLinkUrl", "mindlog://auth/callback?token=" + handoverToken);
                return "auth/app-callback";
            } else {
                log.debug("[AUTH] 웹 로그인 처리 시작...");
                authLoginService.processLogin(code, verifier, request, response);
                session.removeAttribute(PKCE_VERIFIER_KEY);
                log.info("[AUTH] 웹 로그인 성공! /로 리다이렉트");
                return "redirect:/";
            }

        } catch (Exception e) {
            log.error("[AUTH] 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return buildLoginErrorRedirect("login_process_failed", isNativeApp ? "app" : source);
        }
    }

    /**
     * User-Agent를 확인하여 네이티브 앱(Android WebView)에서의 요청인지 판별합니다.
     */
    private boolean isNativeAppRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        log.debug("[AUTH] User-Agent: {}", userAgent);
        if (userAgent == null) {
            return false;
        }
        // Android Custom Tab이나 앱 WebView에서 오는 요청 판별
        // 일반적으로 Custom Tab은 Chrome과 동일한 User-Agent를 사용하므로
        // 세션에 앱 식별자를 저장하는 방식으로 보완
        boolean isWebView = userAgent.contains("wv");
        String loginSource = (String) request.getSession().getAttribute(LOGIN_SOURCE_KEY);
        log.debug("[AUTH] WebView 여부: {}, 세션 login_source: {}", isWebView, loginSource);
        return isWebView || APP_SOURCE.equals(loginSource);
    }

    /**
     * 콜백 URL은 "실제 요청 호스트"를 우선 사용해야 세션 쿠키 도메인과 일치합니다.
     * (예: localhost로 접속했는데 callback만 127.0.0.1이면 세션 쿠키가 끊길 수 있음)
     */
    private String buildRedirectUri(HttpServletRequest request) {
        String forwardedProto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        String host = firstHeaderValue(request.getHeader("Host"));

        String scheme = (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : request.getScheme();
        String authority;
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            authority = forwardedHost;
        } else if (host != null && !host.isBlank()) {
            authority = host;
        } else {
            authority = request.getServerName() + ":" + request.getServerPort();
        }

        // 운영 인증서(CN=www.mindlog.blog)와 콜백 도메인을 일치시켜 인증서 경고를 예방
        if ("https".equalsIgnoreCase(scheme)) {
            if ("mindlog.blog".equalsIgnoreCase(authority)) {
                authority = "www.mindlog.blog";
            } else if (authority.toLowerCase().startsWith("mindlog.blog:")) {
                authority = "www.mindlog.blog" + authority.substring("mindlog.blog".length());
            }
        }

        return scheme + "://" + authority + "/auth/callback";
    }

    private String buildLoginErrorRedirect(String error, String source) {
        if (APP_SOURCE.equals(source)) {
            return "redirect:/auth/login?source=app&error=" + error;
        }
        return "redirect:/auth/login?error=" + error;
    }

    private String firstHeaderValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String extractSessionCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "SESSION".equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String maskSessionId(String sessionId) {
        return Optional.ofNullable(sessionId)
                .map(id -> id.length() <= 12 ? id : id.substring(0, 6) + "..." + id.substring(id.length() - 4))
                .orElse("null");
    }

    private boolean isAlreadyAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private String appendNativeSourceAndVerifier(String redirectUri, String verifier) {
        var encodedVerifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(verifier.getBytes(StandardCharsets.UTF_8));
        return redirectUri + "?source=app&v=" + encodedVerifier;
    }

    private String buildAuthUrl(String provider, String redirectUri, String challenge, String prompt) {
        var encodedRedirectUri = UriUtils.encode(redirectUri, StandardCharsets.UTF_8);
        var encodedChallenge = UriUtils.encode(challenge, StandardCharsets.UTF_8);
        return String.format(
                "%s/auth/v1/authorize?provider=%s&redirect_to=%s&code_challenge=%s&code_challenge_method=S256&flow_type=pkce&prompt=%s",
                supabaseUrl, provider, encodedRedirectUri, encodedChallenge, prompt);
    }
}
