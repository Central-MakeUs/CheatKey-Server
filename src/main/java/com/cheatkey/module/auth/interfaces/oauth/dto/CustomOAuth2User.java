package com.cheatkey.module.auth.interfaces.oauth.dto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.cheatkey.module.auth.interfaces.dto.OAuthUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2User implements OAuth2User {

    private final OAuthUserRequest OAuthUserPrincipal;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
                "kakaoId", OAuthUserPrincipal.getKakaoId(),
                "authStatus", OAuthUserPrincipal.getAuthStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Long getKakaoId() {
        return OAuthUserPrincipal.getKakaoId();
    }

    @Override
    public String getName() {
        return String.valueOf(OAuthUserPrincipal.getKakaoId());
    }
}