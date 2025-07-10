package com.cheatkey.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedirectResolver {

    @Value("${app.frontend-host}")
    private String PROD_HOST;

    private static final String DEV_HOST = "http://localhost:3000";

    public String resolveRedirectBase(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String origin = request.getHeader("Origin");
        String hostHeader = request.getHeader("Host");

        // 로컬 개발 환경 판단 기준: Referer, Origin, Host 중 하나라도 localhost
        boolean isDev =
                (referer != null && referer.contains("localhost")) ||
                        (origin != null && origin.contains("localhost")) ||
                        (hostHeader != null && hostHeader.contains("localhost"));

        return isDev ? DEV_HOST : PROD_HOST;
    }
}
