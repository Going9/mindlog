package com.mindlog.domain.home.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String home(Model model) {
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
        return "home";
    }
}
