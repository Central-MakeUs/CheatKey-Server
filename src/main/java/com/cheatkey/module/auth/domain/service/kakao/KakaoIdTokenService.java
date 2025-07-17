package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.module.auth.domain.service.util.JwtValidationUtil;
import org.springframework.stereotype.Service;

@Service
public class KakaoIdTokenService {
    public static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";

    public String validateToken(String idToken) {return JwtValidationUtil.validateToken(idToken, KAKAO_JWKS_URL);}
} 