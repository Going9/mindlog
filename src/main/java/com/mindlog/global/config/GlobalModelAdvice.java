package com.mindlog.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("isNative")
    public boolean isNative(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && userAgent.contains("Mindlog");
    }
}