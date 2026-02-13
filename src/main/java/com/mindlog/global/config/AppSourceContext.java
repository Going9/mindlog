package com.mindlog.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class AppSourceContext {

    public static final String SOURCE_PARAM = "source";
    public static final String APP_SOURCE = "app";
    private static final String APP_SOURCE_SESSION_KEY = "APP_SOURCE_CONTEXT";

    public boolean isAppSource(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        var source = request.getParameter(SOURCE_PARAM);
        if (APP_SOURCE.equals(source)) {
            rememberAppSource(request);
            return true;
        }

        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(APP_SOURCE_SESSION_KEY));
    }

    public void rememberAppSource(HttpServletRequest request) {
        request.getSession(true).setAttribute(APP_SOURCE_SESSION_KEY, Boolean.TRUE);
    }
}
