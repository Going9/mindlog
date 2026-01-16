package com.mindlog.global.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에서 인증된 사용자의 Profile ID(UUID)를 주입받기 위한 어노테이션.
 *
 * <p>Supabase JWT의 'sub' claim에서 추출한 UUID를 직접 주입합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * @GetMapping("/my-diaries")
 * public String myDiaries(@CurrentProfileId UUID profileId) {
 *     // profileId를 사용하여 해당 사용자의 일기 조회
 * }
 * }</pre>
 *
 * @see org.springframework.security.core.annotation.AuthenticationPrincipal
 */
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentProfileId {
}
