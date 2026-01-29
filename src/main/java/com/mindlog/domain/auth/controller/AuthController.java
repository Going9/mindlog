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
    public String socialLogin(@PathVariable String provider, HttpSession session) {
        // 이미 로그인된 경우 리다이렉트
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/diaries";
        }

        String verifier = PkceUtil.generateCodeVerifier();
        String challenge = PkceUtil.generateCodeChallenge(verifier);
        session.setAttribute("pkce_verifier", verifier);

        String authUrl = String.format(
                "%s/auth/v1/authorize?provider=%s&redirect_to=%s&code_challenge=%s&code_challenge_method=S256&flow_type=pkce",
                supabaseUrl, provider, getRedirectUri(), challenge);

        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (error != null || code == null) {
            log.error("로그인 실패: code={}, error={}", code, error);
            return "redirect:/auth/login?error=auth_failed";
        }

        HttpSession session = request.getSession();
        String verifier = (String) session.getAttribute("pkce_verifier");

        if (verifier == null) {
            return "redirect:/auth/login?error=invalid_session";
        }

        try {
            // ▼▼▼ [딱 한 줄로 끝!] ▼▼▼
            // 복잡한 지지고 볶는 로직은 서비스에게 위임
            authLoginService.processLogin(code, verifier, request, response);

            session.removeAttribute("pkce_verifier");
            return "redirect:/diaries";

        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            return "redirect:/auth/login?error=login_process_failed";
        }
    }
}