package com.cheatkey.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getLoginUserId(HttpServletRequest request) {
        // Swagger mock 인증 우선 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User oAuth2User) {
                Long userId = oAuth2User.getAttribute("kakaoId");
                if (userId != null) return userId;
            }
        }

        // 기본: 세션 기반 처리
        Object kakaoId = request.getSession().getAttribute("loginUser");
        if (kakaoId instanceof Long id) return id;

        throw new IllegalStateException("로그인된 사용자가 없습니다.");
    }
}


