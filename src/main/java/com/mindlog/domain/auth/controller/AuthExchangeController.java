package com.mindlog.domain.auth.controller;

import com.mindlog.domain.auth.service.AuthHandoverService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    private final AuthHandoverService handoverService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/exchange")
    public String exchangeToken(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        log.info("토큰 교환 요청: {}", token.substring(0, Math.min(8, token.length())) + "...");

        var result = handoverService.consumeToken(token);
        if (result == null) {
            log.warn("토큰 교환 실패: 유효하지 않거나 만료된 토큰");
            return "redirect:/auth/login?error=invalid_token";
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result.authentication());
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession();
        result.sessionAttributes().forEach(session::setAttribute);
        log.info("WebView 세션 생성 완료: 사용자 {}", result.authentication().getName());

        model.addAttribute("redirectUrl", "/");
        return "auth/exchange-complete";
    }
}
