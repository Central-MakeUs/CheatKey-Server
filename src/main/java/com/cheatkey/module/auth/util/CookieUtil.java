package com.cheatkey.module.auth.util;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3;

    public static Cookie createRefreshCookie(final String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        // cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        return cookie;
    }

    public static String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new CustomException(ErrorCode.COOKIE_NOT_FOUND);
    }

    // 쿠키 삭제를 위한 메서드
    public static Cookie createExpiredRefreshCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setMaxAge(0); // 즉시 만료
        return cookie;
    }
}