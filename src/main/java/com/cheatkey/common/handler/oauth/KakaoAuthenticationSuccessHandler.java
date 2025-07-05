package com.cheatkey.common.handler.oauth;


import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthRepository authRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
        Long kakaoId = user.getAttribute("kakaoId");

        Optional<Auth> auth = authRepository.findByKakaoId(kakaoId);

        if (auth.isPresent()) {
            Auth requestedAuth = auth.get();
            requestedAuth.increaseLoginCount();
            requestedAuth.updateLastLoginTime(LocalDateTime.now());
            authRepository.save(requestedAuth);

            response.sendRedirect("/home");

        } else {
            response.sendRedirect("/auth/register");
        }
    }
}
