package com.mindlog.global.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        // @CurrentProfileId 어노테이션이 있고 + 타입이 UUID인 경우만 동작
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

        // 1. 인증 정보 확인
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다. (로그인이 필요합니다)");
        }

        Object principal = authentication.getPrincipal();

        // 2. Principal이 String(UUID 문자열)인지 확인
        // AuthController에서 UsernamePasswordAuthenticationToken의 첫 번째 인자로 넣은 값이 여기에 들어옵니다.
        if (principal instanceof String userIdStr) {
            try {
                return UUID.fromString(userIdStr); // String -> UUID 변환
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Principal이 유효한 UUID 형식이 아닙니다: " + userIdStr);
            }
        }

        // 3. 만약 타입이 String이 아니라면 에러 (디버깅용)
        throw new IllegalStateException(
                "지원하지 않는 Principal 타입입니다. 현재 타입: " + principal.getClass().getName()
        );
    }
}