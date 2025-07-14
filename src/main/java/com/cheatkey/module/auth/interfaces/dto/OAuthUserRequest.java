package com.cheatkey.module.auth.interfaces.dto;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthUserRequest {
    private Long kakaoId;
    private AuthStatus authStatus;

    public OAuthUserRequest(Auth auth) {
        this.kakaoId = auth.getKakaoId();
        this.authStatus = auth.getAuthStatus();
    }

    @Builder
    public OAuthUserRequest(Long kakaoId, AuthStatus authStatus) {
        this.kakaoId = kakaoId;
        this.authStatus = authStatus;
    }
}
