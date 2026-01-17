package com.mindlog.global.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
@NullMarked
public class CurrentProfileIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentProfileId.class) &&
                parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject(); // Supabase의 유저 ID(UUID)
            return UUID.fromString(subject);
        }

        // 2. Jwt 객체인 경우 (OAuth2 Resource Server 기본 동작)
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject == null) {
                throw new IllegalStateException("JWT에 'sub' 클레임이 존재하지 않습니다.");
            }
            return UUID.fromString(subject);
        }

        throw new IllegalStateException(
                "지원하지 않는 Principal 타입입니다. 현재 타입: " + principal.getClass().getName()
        );
    }
}