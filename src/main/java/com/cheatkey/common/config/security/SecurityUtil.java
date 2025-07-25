package com.cheatkey.common.config.security;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * 현재 SecurityContext에서 userId(String)를 반환
     * 인증이 없거나 userId가 없으면 예외 처리
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String userId) {
            return userId;
        }
        throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
    }
} 