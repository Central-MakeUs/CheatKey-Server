package com.cheatkey.module.auth.domain.service.apple;

import com.cheatkey.module.auth.domain.service.util.JwtValidationUtil;
import org.springframework.stereotype.Service;

@Service
public class AppleIdTokenService {
    public static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    public String validateToken(String idToken) {
        return JwtValidationUtil.validateToken(idToken, APPLE_JWKS_URL);
    }
} 