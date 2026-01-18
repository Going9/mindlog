package com.mindlog.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에서 인증된 사용자의 Profile ID(UUID)를 주입받기 위한 커스텀 어노테이션.
 *
 * <p>기존의 @AuthenticationPrincipal을 제거하고,
 * WebMvcConfig에 등록된 CurrentProfileIdArgumentResolver가 이 어노테이션을 인식하여 처리합니다.</p>
 *
 * <p>SecurityContext의 Principal(String)을 가져와 UUID로 변환하여 주입합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * @GetMapping("/my-diaries")
 * public String myDiaries(@CurrentProfileId UUID profileId) {
 * // profileId를 사용하여 해당 사용자의 일기 조회
 * }
 * }</pre>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentProfileId {
    // @AuthenticationPrincipal 제거됨! 이제 순수 마커 인터페이스입니다.
}