package com.cheatkey.common.config.security.filter;

import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.JwtAuthenticationException;
import com.cheatkey.module.auth.interfaces.dto.AppUserDetails;
import com.cheatkey.module.auth.interfaces.dto.AppUserDetailsRequest;
import com.cheatkey.common.jwt.JwtUtil;
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

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(RequestMatcher matcher, JwtUtil jwtUtil) {
        super(matcher);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // 요청 헤더에서 Authorization 값 추출
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Authorization header is missing or does not start with Bearer");
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        // "Bearer " 이후의 토큰 값만 추출
        String token = authorizationHeader.substring(7).trim();
        log.info("Token extracted from header: {}", token);

        // 사용자 정보 추출 및 인증 객체 생성
        AppUserDetailsRequest detailsRequest = createPrincipalDetailsRequest(token);
        AppUserDetails appUserDetails = new AppUserDetails(detailsRequest);

        return new UsernamePasswordAuthenticationToken(appUserDetails, null, appUserDetails.getAuthorities());
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

    private AppUserDetailsRequest createPrincipalDetailsRequest(String token) {
        return AppUserDetailsRequest.of(jwtUtil.getKakaoId(token));
    }
}
