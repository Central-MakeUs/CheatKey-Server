package com.cheatkey.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class SecurityUtil {

    private SecurityUtil() {
        // 유틸 클래스는 인스턴스화 방지
    }

    public static Long getLoginUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("현재 로그인된 사용자가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            Long userId = oAuth2User.getAttribute("kakaoId");

            if (userId != null) {
                return userId;
            }

            throw new IllegalStateException("OAuth2User에서 userId를 찾을 수 없습니다.");
        }

        throw new IllegalStateException("알 수 없는 사용자 인증 정보입니다.");
    }
}

