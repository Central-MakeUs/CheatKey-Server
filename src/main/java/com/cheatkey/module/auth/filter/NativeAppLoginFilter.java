package com.cheatkey.module.auth.filter;

import com.cheatkey.module.auth.interfaces.dto.AppUserDetails;
import com.cheatkey.common.jwt.JwtUtil;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
public class NativeAppLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final Long ACCESS_TOKEN_EXPIRATION_TIME = 1000L * 60 * 60 * 24; // 24시간
    private static final Long REFRESH_TOKEN_EXPIRATION_TIME = 1000L * 60 * 60 * 24; // 24시간
    private static final String ACCESS_HEADER_NAME = "Authorization";
    private static final String REFRESH_HEADER_NAME = "Refresh-Token";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws
            AuthenticationException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Auth auth = mapper.readValue(request.getInputStream(), Auth.class);
            log.info("attemptAuthentication:: auth kakaoId: {}", auth.getKakaoId());
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    auth.getKakaoId(),
                    null
            );
            return authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            log.error("attemptAuthentication Exception occur: {}", e.getMessage());
            throw new AuthenticationException("로그인 시도에 실패했습니다.") {
            };
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authentication) {
        AppUserDetails appUserDetails = (AppUserDetails)authentication.getPrincipal();

        Long kakaoId = appUserDetails.getKakaoId();

        String access = jwtUtil.createJwt("access", kakaoId, ACCESS_TOKEN_EXPIRATION_TIME);
        String refresh = jwtUtil.createJwt("refresh", kakaoId, REFRESH_TOKEN_EXPIRATION_TIME);

        response.addHeader(ACCESS_HEADER_NAME, TOKEN_PREFIX + access);
        response.addHeader(REFRESH_HEADER_NAME, TOKEN_PREFIX + refresh);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException authenticationException) throws IOException {
        log.error("Authentication not successful: {}", authenticationException.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authenticationException.getMessage());
        response.setStatus(401);
    }
}
