package com.cheatkey.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SwaggerMockOAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private static final String[] EXCLUDE_PATH_PATTERNS = {
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (!isWhitePath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 최소 mock 사용자 정보: nickname, kakaoId
        Map<String, Object> kakaoAccount = Map.of(
                "profile", Map.of("nickname", "swagger_mock_user")
        );

        OAuth2User mockUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "kakao_account", kakaoAccount,
                        "kakaoId", 999999999L
                ),
                "kakaoId" // nameAttributeKey
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug(">>> SwaggerMockOAuthFilter: Mock user injected into SecurityContext");

        filterChain.doFilter(request, response);
    }

    private boolean isWhitePath(String uri) {
        for (String pattern : EXCLUDE_PATH_PATTERNS) {
            if (antPathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
