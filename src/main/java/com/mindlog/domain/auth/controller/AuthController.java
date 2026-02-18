package com.mindlog.domain.auth.controller;

import com.mindlog.domain.auth.service.AuthLoginService;
import com.mindlog.global.security.PkceUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return "auth/login";
    }

    @GetMapping("/login/{provider}")
    public String socialLogin(
            @PathVariable String provider,
            @RequestParam(name = "source", required = false) String source,
            HttpServletRequest request,
            HttpSession session
    ) {
        if (isAlreadyAuthenticated()) {
            return "redirect:/";
        }

        var verifier = PkceUtil.generateCodeVerifier();
        var challenge = PkceUtil.generateCodeChallenge(verifier);
        session.setAttribute(PKCE_VERIFIER_KEY, verifier);

        var isNativeApp = APP_SOURCE.equals(source);
        if (isNativeApp) {
            session.setAttribute(LOGIN_SOURCE_KEY, APP_SOURCE);
        }

        var prompt = "google".equalsIgnoreCase(provider) ? "select_account" : "login";
        var redirectUri = buildRedirectUri(request);

        if (isNativeApp) {
            redirectUri = appendNativeSourceAndVerifier(redirectUri, verifier);
        }

        var authUrl = buildAuthUrl(provider, redirectUri, challenge, prompt);
        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "v", required = false) String encodedVerifier,
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.ui.Model model
    ) {
        if (error != null || code == null) {
            return buildLoginErrorRedirect("auth_failed", source);
        }

        var session = request.getSession();
        var hasEncodedVerifier = encodedVerifier != null && !encodedVerifier.isEmpty();
        var isNativeApp = APP_SOURCE.equals(source)
                || hasEncodedVerifier
                || APP_SOURCE.equals(session.getAttribute(LOGIN_SOURCE_KEY));

        String verifier;
        if (hasEncodedVerifier) {
            verifier = decodeVerifier(encodedVerifier);
            if (verifier == null) {
                session.removeAttribute(LOGIN_SOURCE_KEY);
                return buildLoginErrorRedirect("invalid_session", isNativeApp ? APP_SOURCE : source);
            }
        } else {
            verifier = (String) session.getAttribute(PKCE_VERIFIER_KEY);
        }

        if (verifier == null) {
            session.removeAttribute(LOGIN_SOURCE_KEY);
            return buildLoginErrorRedirect("invalid_session", isNativeApp ? APP_SOURCE : source);
        }

        try {
            if (isNativeApp) {
                var handoverToken = authLoginService.processLoginForNativeApp(code, verifier);
                session.removeAttribute(PKCE_VERIFIER_KEY);
                session.removeAttribute(LOGIN_SOURCE_KEY);

                model.addAttribute("deepLinkUrl", "mindlog://auth/callback?token=" + handoverToken);
                return "auth/app-callback";
            }

            authLoginService.processLogin(code, verifier, request, response);
            session.removeAttribute(PKCE_VERIFIER_KEY);
            session.removeAttribute(LOGIN_SOURCE_KEY);
            return "redirect:/";
        } catch (Exception e) {
            log.error("[AUTH] 로그인 처리 중 오류", e);
            session.removeAttribute(LOGIN_SOURCE_KEY);
            return buildLoginErrorRedirect("login_process_failed", isNativeApp ? APP_SOURCE : source);
        }
    }

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

    private String decodeVerifier(String encodedVerifier) {
        try {
            return new String(Base64.getUrlDecoder().decode(encodedVerifier), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            log.warn("[AUTH] 유효하지 않은 verifier 인코딩 값");
            return null;
        }
    }

    private String buildAuthUrl(String provider, String redirectUri, String challenge, String prompt) {
        var encodedRedirectUri = UriUtils.encode(redirectUri, StandardCharsets.UTF_8);
        var encodedChallenge = UriUtils.encode(challenge, StandardCharsets.UTF_8);
        return String.format(
                "%s/auth/v1/authorize?provider=%s&redirect_to=%s&code_challenge=%s&code_challenge_method=S256&flow_type=pkce&prompt=%s",
                supabaseUrl,
                provider,
                encodedRedirectUri,
                encodedChallenge,
                prompt
        );
    }
}
