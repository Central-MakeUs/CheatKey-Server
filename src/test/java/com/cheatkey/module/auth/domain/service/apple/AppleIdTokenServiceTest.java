package com.cheatkey.module.auth.domain.service.apple;

import com.cheatkey.module.auth.domain.service.util.JwtValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AppleIdTokenServiceTest {

    private AppleIdTokenService appleIdTokenService;

    @BeforeEach
    void setUp() {
        appleIdTokenService = new AppleIdTokenService();
    }

    @Test
    void 유효한_idToken_검증_성공() {
        // given
        String idToken = "valid.apple.id.token";
        String expectedProviderId = "apple123";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(idToken, AppleIdTokenService.APPLE_JWKS_URL))
                    .thenReturn(expectedProviderId);

            // when
            String result = appleIdTokenService.validateToken(idToken);

            // then
            assertEquals(expectedProviderId, result);
            mockedJwtValidation.verify(() -> JwtValidationUtil.validateToken(idToken, AppleIdTokenService.APPLE_JWKS_URL));
        }
    }

    @Test
    void 유효하지_않은_idToken_검증_실패() {
        // given
        String invalidIdToken = "invalid.token";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(invalidIdToken, AppleIdTokenService.APPLE_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                appleIdTokenService.validateToken(invalidIdToken);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }

    @Test
    void null_idToken_전달시_예외발생() {
        // given
        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(null, AppleIdTokenService.APPLE_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                appleIdTokenService.validateToken(null);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }

    @Test
    void 빈_idToken_전달시_예외발생() {
        // given
        String emptyIdToken = "";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(emptyIdToken, AppleIdTokenService.APPLE_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                appleIdTokenService.validateToken(emptyIdToken);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }
} 