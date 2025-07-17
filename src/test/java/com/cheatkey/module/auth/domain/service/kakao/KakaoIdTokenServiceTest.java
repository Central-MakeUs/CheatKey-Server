package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.module.auth.domain.service.util.JwtValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class KakaoIdTokenServiceTest {

    private KakaoIdTokenService kakaoIdTokenService;

    @BeforeEach
    void setUp() {
        kakaoIdTokenService = new KakaoIdTokenService();
    }

    @Test
    void 유효한_idToken_검증_성공() {
        // given
        String idToken = "valid.kakao.id.token";
        String expectedProviderId = "kakao123";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(idToken, KakaoIdTokenService.KAKAO_JWKS_URL))
                    .thenReturn(expectedProviderId);

            // when
            String result = kakaoIdTokenService.validateToken(idToken);

            // then
            assertEquals(expectedProviderId, result);
            mockedJwtValidation.verify(() -> JwtValidationUtil.validateToken(idToken, KakaoIdTokenService.KAKAO_JWKS_URL));
        }
    }

    @Test
    void 유효하지_않은_idToken_검증_실패() {
        // given
        String invalidIdToken = "invalid.token";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(invalidIdToken, KakaoIdTokenService.KAKAO_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                kakaoIdTokenService.validateToken(invalidIdToken);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }

    @Test
    void null_idToken_전달시_예외발생() {
        // given
        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(null, KakaoIdTokenService.KAKAO_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                kakaoIdTokenService.validateToken(null);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }

    @Test
    void 빈_idToken_전달시_예외발생() {
        // given
        String emptyIdToken = "";

        try (MockedStatic<JwtValidationUtil> mockedJwtValidation = Mockito.mockStatic(JwtValidationUtil.class)) {
            mockedJwtValidation.when(() -> JwtValidationUtil.validateToken(emptyIdToken, KakaoIdTokenService.KAKAO_JWKS_URL))
                    .thenThrow(new RuntimeException("토큰 검증 실패"));

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                kakaoIdTokenService.validateToken(emptyIdToken);
            });
            
            assertTrue(ex.getMessage().contains("토큰 검증 실패"));
        }
    }
} 