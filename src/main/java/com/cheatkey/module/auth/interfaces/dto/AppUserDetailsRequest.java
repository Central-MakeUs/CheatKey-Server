package com.cheatkey.module.auth.interfaces.dto;

import com.cheatkey.module.auth.domain.entity.Auth;

public record AppUserDetailsRequest(Long kakaoId) {
    public static AppUserDetailsRequest of(Long kakaoId) {
        return new AppUserDetailsRequest(kakaoId);
    }

    public static AppUserDetailsRequest from(Auth user) {
        return new AppUserDetailsRequest(user.getKakaoId());
    }
}