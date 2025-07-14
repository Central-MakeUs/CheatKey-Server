package com.cheatkey.module.auth.interfaces.handler;

import java.io.IOException;

import com.cheatkey.module.auth.interfaces.oauth.dto.CustomOAuth2User;
import com.cheatkey.module.auth.util.CookieUtil;
import com.cheatkey.common.jwt.JwtUtil;
import com.cheatkey.module.auth.domain.service.RefreshManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshManager refreshManager;

    private static final String ACCESS_HEADER_NAME = "Authorization";
    private static final String REFRESH_HEADER_NAME = "Refresh-Token";
    private static final String TOKEN_PREFIX = "Bearer";
    private static final Long ACCESS_TOKEN_EXP = 604800000L; // 일주일
    private static final Long REFRESH_TOKEN_EXP = 86400000L;

    // @TODO 도메인 수정
    private static final String REDIRECT_URL = "http://43.203.30.24/login/kakao";
    private static final String DEV_ORIGIN = "http://localhost";
    private static final String DEV_REDIRECT_URL = "http://localhost:3000/login/kakao";

    // 개발 환경 체크 함수 - 추후 제거 예정
    private boolean isDevelopment(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        log.info("Referer: {}", referer);
        return referer != null && referer.startsWith(DEV_ORIGIN);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User)authentication.getPrincipal();

        Long kakaoId = customOAuth2User.getKakaoId();

        String access = jwtUtil.createJwt("access", kakaoId, ACCESS_TOKEN_EXP);
        String refresh = jwtUtil.createJwt("refresh", kakaoId, REFRESH_TOKEN_EXP);

        refreshManager.addRefreshEntity(kakaoId, refresh, 86400000L);

        log.info("Access Token : {} ", TOKEN_PREFIX + " " + access);
        log.info("Refresh Token : {} ", TOKEN_PREFIX + " " + refresh);

        // 엑세스 토큰과 리프레시 토큰을 응답 헤더와 쿠키에 설정
        response.setHeader(ACCESS_HEADER_NAME, TOKEN_PREFIX + " " + access);
        Cookie refreshCookie = CookieUtil.createRefreshCookie(refresh);
        response.addCookie(refreshCookie);
        log.info("Refresh Cookie: {}", refreshCookie.getName() + "=" + refreshCookie.getValue());

        // 로컬 환경에서 개발할 때는 로컬로 리다이렉트 되도록 설정
        String finalRedirectUrl = isDevelopment(request) ? DEV_REDIRECT_URL : REDIRECT_URL;
        log.info("Redirect URL: {}", finalRedirectUrl);
        response.sendRedirect(finalRedirectUrl + "?accessToken=" + access + "&refreshToken=" + refresh);
    }
}
