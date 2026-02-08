package com.mindlog.domain.home.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 홈 페이지 Controller.
 */
@Controller
public class HomeController {

    @Value("${mindlog.supabase.url}")
    private String supabaseUrl;

    @Value("${mindlog.supabase.anon-key}")
    private String supabaseAnonKey;

    @GetMapping("/")
    public String home(Model model, HttpServletResponse response) {
        // Turbo 캐시 무효화: 로그인/로그아웃 상태가 변경되어도 항상 최신 페이지 표시
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
        return "home";
    }
}
