package com.cheatkey.common.config.security.filter;

import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.JwtAuthenticationException;
import com.cheatkey.common.jwt.JwtProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(RequestMatcher matcher, JwtProvider jwtProvider) {
        super(matcher);
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Authorization header is missing or does not start with Bearer");
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        String token = authorizationHeader.substring(7).trim();
        log.info("Token extracted from header: {}", token);

        // JwtProvider를 통해 토큰 검증 및 사용자 정보 추출
        if (!jwtProvider.validateToken(token)) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        // 사용자 정보 추출 (예: userId, provider 등)
        String userId = jwtProvider.getUserIdFromToken(token); // JwtProvider에 맞게 구현

        // 인증 객체 생성 (권한이 있다면 authorities도 추가)
        return new UsernamePasswordAuthenticationToken(userId, null, null);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authentication) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException authenticationException) throws IOException {
        SecurityContextHolder.clearContext();
        log.error("Authentication not successful: {}", authenticationException.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authenticationException.getMessage());
    }
}
