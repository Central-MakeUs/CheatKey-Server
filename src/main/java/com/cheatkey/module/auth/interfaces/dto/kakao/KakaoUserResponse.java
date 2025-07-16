package com.cheatkey.module.auth.interfaces.dto.kakao;

public class KakaoUserResponse {
    public KakaoAccount kakaoAccount;
    public static class KakaoAccount {
        public String email;
    }
}