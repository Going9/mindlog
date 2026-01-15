package com.mindlog.global.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 인증 관련 페이지를 제공하는 Controller.
 * Supabase Auth를 사용한 소셜 로그인(구글, 카카오)을 지원합니다.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @Value("${mindlog.supabase.anon-key}")
    private String supabaseAnonKey;

    /**
     * 로그인 페이지를 반환합니다.
     * 구글, 카카오 소셜 로그인 버튼을 포함합니다.
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
        return "auth/login";
    }

    /**
     * OAuth 콜백 페이지를 반환합니다.
     * Supabase OAuth 플로우 완료 후 리다이렉트되는 페이지입니다.
     */
    @GetMapping("/callback")
    public String callbackPage(Model model) {
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
        return "auth/callback";
    }

    /**
     * 로그아웃 페이지를 반환합니다.
     * 클라이언트에서 Supabase 세션을 정리한 후 로그인 페이지로 리다이렉트합니다.
     */
    @GetMapping("/logout")
    public String logoutPage(Model model) {
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
        return "auth/logout";
    }
}
