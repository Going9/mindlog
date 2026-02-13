package com.mindlog.global.config;

import com.mindlog.domain.profile.entity.Profile;
import com.mindlog.domain.profile.repository.ProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final ProfileRepository profileRepository;
    private final AppSourceContext appSourceContext;

    @ModelAttribute("isNative")
    public boolean isNative(HttpServletRequest request) {
        return appSourceContext.isAppSource(request);
    }

    @ModelAttribute("isTurboFrame")
    public boolean isTurboFrame(HttpServletRequest request) {
        return request.getHeader("Turbo-Frame") != null;
    }

    @ModelAttribute("viewLayout")
    public String viewLayout(HttpServletRequest request) {
        return (request.getHeader("Turbo-Frame") != null) ? "layout/turbo" : "layout/base";
    }

    @ModelAttribute("userName")
    public String userName(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String cachedName = (String) session.getAttribute("USER_NAME");
            if (cachedName != null && !cachedName.isBlank()) {
                return cachedName;
            }
        }

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String principalName = authentication.getName();

        try {
            UUID profileId = UUID.fromString(principalName);
            Profile profile = profileRepository.findById(profileId).orElse(null);
            if (profile != null) {
                String name = profile.getName();
                if (name != null && !name.isBlank()) {
                    if (session != null) {
                        session.setAttribute("USER_NAME", name);
                    }
                    return name;
                }
            }
        } catch (Exception e) {
        }

        return principalName;
    }
}
