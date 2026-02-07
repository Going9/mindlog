package com.mindlog.domain.auth.controller;

import com.mindlog.domain.auth.service.AuthLoginService;
import com.mindlog.global.security.PkceUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthLoginService authLoginService;

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @Value("${mindlog.serverIp}")
    private String serverIp;

    @Value("${mindlog.serverPort}")
    private String serverPort;

    @GetMapping("/login")
    public String loginPage() {
        log.info("[AUTH] 로그인 페이지 요청");
        return "auth/login";
    }

    /**
     * 환경에 맞는 리다이렉트 URI 생성
     * 로컬: http://localhost:8080/auth/callback
     * 운영: https://도메인/auth/callback
     */
    private String getRedirectUri() {
        if (serverIp.equals("localhost") || serverIp.equals("127.0.0.1")) {
            return "http://" + serverIp + ":" + serverPort + "/auth/callback";
        }
        // 운영 환경은 클라우드타입이 HTTPS를 제공하므로 https로 강제 설정
        return "https://" + serverIp + "/auth/callback";
    }

    @GetMapping("/login/{provider}")
    public String socialLogin(@PathVariable String provider,
            @RequestParam(name = "source", required = false) String source,
            HttpSession session) {
        log.info("[AUTH] ===== 소셜 로그인 시작 =====");
        log.info("[AUTH] provider={}, source={}", provider, source);

        // 이미 로그인된 경우 리다이렉트
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            log.info("[AUTH] 이미 로그인됨 - /diaries로 리다이렉트");
            return "redirect:/diaries";
        }

        String verifier = PkceUtil.generateCodeVerifier();
        String challenge = PkceUtil.generateCodeChallenge(verifier);
        session.setAttribute("pkce_verifier", verifier);
        log.info("[AUTH] PKCE verifier 생성 완료 (길이: {})", verifier.length());

        // 네이티브 앱에서 요청한 경우 source를 세션에 저장 (WebView용 백업)
        boolean isNativeApp = "app".equals(source);
        if (isNativeApp) {
            session.setAttribute("login_source", "app");
            log.info("[AUTH] 네이티브 앱 요청 - source=app");
        }

        String prompt = "google".equalsIgnoreCase(provider) ? "select_account" : "login";

        // redirect_to URL에 source 파라미터 포함 (Custom Tab은 별도 세션이므로)
        String redirectUri = getRedirectUri();
        log.info("[AUTH] 기본 redirectUri: {}", redirectUri);

        if (isNativeApp) {
            // 앱용: verifier를 URL에 포함 (Custom Tab은 세션을 공유하지 않으므로)
            // Base64 인코딩하여 URL-safe하게 전달
            String encodedVerifier = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(verifier.getBytes());
            redirectUri += "?source=app&v=" + encodedVerifier;
            log.info("[AUTH] 앱용 redirectUri 생성 완료 (verifier 포함)");
        }

        String authUrl = String.format(
                "%s/auth/v1/authorize?provider=%s&redirect_to=%s&code_challenge=%s&code_challenge_method=S256&flow_type=pkce&prompt=%s",
                supabaseUrl, provider, redirectUri, challenge, prompt);

        log.info("[AUTH] OAuth URL로 리다이렉트: {}", authUrl.substring(0, Math.min(100, authUrl.length())) + "...");
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

        log.info("[AUTH] ===== OAuth 콜백 수신 =====");
        log.info("[AUTH] code={}, error={}, source={}, v(verifier)={}",
                code != null ? "있음(길이:" + code.length() + ")" : "없음",
                error,
                source,
                encodedVerifier != null ? "있음(길이:" + encodedVerifier.length() + ")" : "없음");

        if (error != null || code == null) {
            log.error("[AUTH] 로그인 실패: code={}, error={}", code, error);
            return "redirect:/auth/login?error=auth_failed";
        }

        HttpSession session = request.getSession();

        // 네이티브 앱 여부 판별 (source 파라미터 또는 User-Agent로 판단)
        boolean isNativeApp = "app".equals(source) || isNativeAppRequest(request);
        log.info("[AUTH] 네이티브 앱 여부: {}", isNativeApp);

        // verifier 결정: URL 파라미터 > 세션
        String verifier;
        if (encodedVerifier != null && !encodedVerifier.isEmpty()) {
            // 앱용: URL에서 verifier 디코딩
            verifier = new String(java.util.Base64.getUrlDecoder().decode(encodedVerifier));
            log.info("[AUTH] URL 파라미터에서 verifier 추출 성공 (길이: {})", verifier.length());
        } else {
            // 웹용: 세션에서 verifier 추출
            verifier = (String) session.getAttribute("pkce_verifier");
            log.info("[AUTH] 세션에서 verifier 추출: {}", verifier != null ? "성공" : "실패");
        }

        if (verifier == null) {
            log.error("[AUTH] verifier를 찾을 수 없음 - source={}, encodedVerifier={}", source, encodedVerifier);
            return "redirect:/auth/login?error=invalid_session";
        }

        try {
            if (isNativeApp) {
                log.info("[AUTH] 네이티브 앱 로그인 처리 시작...");
                // 네이티브 앱: 일회용 토큰을 생성하고 딥링크로 리다이렉트
                String handoverToken = authLoginService.processLoginForNativeApp(code, verifier);
                session.removeAttribute("pkce_verifier");

                log.info("[AUTH] 네이티브 앱 로그인 성공! handoverToken 생성 완료");
                log.info("[AUTH] 딥링크 URL: mindlog://auth/callback?token={}...",
                        handoverToken.substring(0, Math.min(10, handoverToken.length())));

                // Custom Tab에서 딥링크가 작동하도록 HTML 페이지 반환
                model.addAttribute("deepLinkUrl", "mindlog://auth/callback?token=" + handoverToken);
                return "auth/app-callback";
            } else {
                log.info("[AUTH] 웹 로그인 처리 시작...");
                // 웹: 기존대로 처리
                authLoginService.processLogin(code, verifier, request, response);
                session.removeAttribute("pkce_verifier");
                log.info("[AUTH] 웹 로그인 성공! /diaries로 리다이렉트");
                return "redirect:/diaries";
            }

        } catch (Exception e) {
            log.error("[AUTH] 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return "redirect:/auth/login?error=login_process_failed";
        }
    }

    /**
     * User-Agent를 확인하여 네이티브 앱(Android WebView)에서의 요청인지 판별합니다.
     */
    private boolean isNativeAppRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        log.info("[AUTH] User-Agent: {}", userAgent);
        if (userAgent == null) {
            return false;
        }
        // Android Custom Tab이나 앱 WebView에서 오는 요청 판별
        // 일반적으로 Custom Tab은 Chrome과 동일한 User-Agent를 사용하므로
        // 세션에 앱 식별자를 저장하는 방식으로 보완
        boolean isWebView = userAgent.contains("wv");
        String loginSource = (String) request.getSession().getAttribute("login_source");
        log.info("[AUTH] WebView 여부: {}, 세션 login_source: {}", isWebView, loginSource);
        return isWebView || "app".equals(loginSource);
    }
}