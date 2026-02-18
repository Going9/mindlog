package com.mindlog.domain.auth.controller;

import com.mindlog.domain.auth.service.AuthHandoverService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 네이티브 앱(Android)의 토큰 교환을 처리하는 컨트롤러.
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthExchangeController {

    static final String LAST_EXCHANGE_TOKEN_FINGERPRINT = "LAST_EXCHANGE_TOKEN_FINGERPRINT";

    private final AuthHandoverService handoverService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/exchange")
    public String exchangeToken(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        var tokenPreview = token.isBlank() ? "(blank)" : token.substring(0, Math.min(8, token.length())) + "...";
        log.info("토큰 교환 요청: {}", tokenPreview);
        HttpSession session = request.getSession();
        var tokenFingerprint = tokenFingerprint(token);
        var lastTokenFingerprint = (String) session.getAttribute(LAST_EXCHANGE_TOKEN_FINGERPRINT);

        if (tokenFingerprint.equals(lastTokenFingerprint) && isAlreadyAuthenticated()) {
            log.info("중복 토큰 교환 요청을 무시하고 홈으로 복귀합니다.");
            model.addAttribute("redirectUrl", "/");
            return "auth/exchange-complete";
        }

        var result = handoverService.consumeToken(token);
        if (result == null) {
            if (isAlreadyAuthenticated()) {
                log.info("토큰 교환 실패이지만 이미 인증 상태이므로 홈으로 복귀합니다.");
                return "redirect:/";
            }
            log.warn("토큰 교환 실패: 유효하지 않거나 만료된 토큰");
            return "redirect:/auth/login?source=app&error=invalid_token";
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result.authentication());
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);

        session.setAttribute(LAST_EXCHANGE_TOKEN_FINGERPRINT, tokenFingerprint);
        result.sessionAttributes().forEach(session::setAttribute);
        log.info("WebView 세션 생성 완료: 사용자 {}", result.authentication().getName());
        model.addAttribute("redirectUrl", "/");
        return "auth/exchange-complete";
    }

    private boolean isAlreadyAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private String tokenFingerprint(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.warn("토큰 fingerprint 생성 실패");
            return token;
        }
    }
}
