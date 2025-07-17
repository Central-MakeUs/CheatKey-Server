package com.cheatkey.module.auth.domain.service.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class JwtValidationUtilTest {

    @Test
    void 유효하지_않은_idToken_검증시_예외발생() {
        // given
        String invalidIdToken = "invalid.token.here";
        String jwksUrl = "https://kauth.kakao.com/.well-known/jwks.json";

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            JwtValidationUtil.validateToken(invalidIdToken, jwksUrl);
        });
        
        assertTrue(ex.getMessage().contains("토큰 검증 실패"));
    }

    @Test
    void 잘못된_JWKS_URL_사용시_예외발생() {
        // given
        String idToken = "some.token.here";
        String invalidJwksUrl = "https://invalid.url/jwks.json";

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            JwtValidationUtil.validateToken(idToken, invalidJwksUrl);
        });
        
        assertTrue(ex.getMessage().contains("토큰 검증 실패"));
    }

    @Test
    void null_idToken_전달시_예외발생() {
        // given
        String jwksUrl = "https://kauth.kakao.com/.well-known/jwks.json";

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            JwtValidationUtil.validateToken(null, jwksUrl);
        });
        
        assertTrue(ex.getMessage().contains("토큰 검증 실패"));
    }

    @Test
    void null_JWKS_URL_전달시_예외발생() {
        // given
        String idToken = "some.token.here";

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            JwtValidationUtil.validateToken(idToken, null);
        });
        
        assertTrue(ex.getMessage().contains("토큰 검증 실패"));
    }
} 