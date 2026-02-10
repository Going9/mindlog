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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 네이티브 앱(Android)의 토큰 교환을 처리하는 컨트롤러.
 * 
 * Android WebView가 이 엔드포인트에 접속하면,
 * 일회용 토큰을 검증하고 WebView용 세션을 생성합니다.
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthExchangeController {

    private final AuthHandoverService handoverService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * 토큰을 교환하여 WebView 세션을 생성합니다.
     * 
     * @param token 일회용 토큰
     * @return 성공 시 홈으로 리다이렉트, 실패 시 로그인 페이지로 이동
     */
    @GetMapping("/exchange")
    public String exchangeToken(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("토큰 교환 요청: {}", token.substring(0, Math.min(8, token.length())) + "...");
        log.info("토큰 교환 요청 세션 정보 - requestedSessionId={}, valid={}",
                request.getRequestedSessionId(),
                request.isRequestedSessionIdValid());

        // 1. 토큰 검증 및 인증 정보 조회
        var result = handoverService.consumeToken(token);

        if (result == null) {
            log.warn("토큰 교환 실패: 유효하지 않거나 만료된 토큰");
            return "redirect:/auth/login?error=invalid_token";
        }

        // 2. 현재 스레드(WebView 요청 처리 중)의 SecurityContext에 인증 정보 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result.authentication());
        SecurityContextHolder.setContext(context);

        // 3. SecurityContext를 저장 (세션에 저장됨 → JSESSIONID 쿠키가 의미를 가짐)
        securityContextRepository.saveContext(context, request, response);

        // 4. 세션에 추가 속성 저장 (ACCESS_TOKEN, USER_NAME 등)
        HttpSession session = request.getSession();
        result.sessionAttributes().forEach(session::setAttribute);
        log.info("토큰 교환 후 세션 생성 - sessionId={}", session.getId());

        log.info("WebView 세션 생성 완료: 사용자 {}", result.authentication().getName());

        // 5. 로그인 완료 후 홈으로 이동
        return "redirect:/diaries";
    }
}
