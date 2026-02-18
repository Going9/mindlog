package com.mindlog.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${server.servlet.session.cookie.name:SESSION}") String cookieName,
            @Value("${server.servlet.session.cookie.http-only:true}") boolean httpOnly,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure,
            @Value("${server.servlet.session.cookie.same-site:lax}") String sameSite,
            @Value("${server.servlet.session.cookie.path:/}") String path,
            @Value("${server.servlet.session.cookie.domain:}") String domain
    ) {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setUseHttpOnlyCookie(httpOnly);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(normalizeSameSite(sameSite));
        serializer.setCookiePath(path);

        if (domain != null && !domain.isBlank()) {
            serializer.setDomainName(domain);
        }

        return serializer;
    }

    private String normalizeSameSite(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Lax";
        }

        return switch (sameSite.trim().toLowerCase()) {
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
