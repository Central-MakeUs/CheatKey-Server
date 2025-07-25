package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private AuthRepository authRepository;
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰 재발급 성공")
    void refreshAccessToken_success() {
        // given
        String refreshToken = "validRefreshToken";
        Long userId = 1L;
        Auth mockAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        String newAccessToken = "newAccessToken";

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(refreshToken)).thenReturn(userId.toString());
        when(authRepository.findById(userId)).thenReturn(Optional.of(mockAuth));
        when(jwtProvider.createAccessToken(anyLong(), any(), any())).thenReturn(newAccessToken);

        // when
        SignInResponse response = authService.refreshAccessToken(refreshToken);

        // then
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("ACTIVE", response.getUserState());
        assertEquals("Bearer", response.getGrantType());
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 예외 발생")
    void refreshAccessToken_invalidToken() {
        // given
        String refreshToken = "invalidRefreshToken";
        when(jwtProvider.validateToken(refreshToken)).thenReturn(false);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                authService.refreshAccessToken(refreshToken)
        );
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("리프레시 토큰은 유효하지만 유저가 없으면 예외 발생")
    void refreshAccessToken_userNotFound() {
        // given
        String refreshToken = "validRefreshToken";
        Long userId = 1L;
        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(refreshToken)).thenReturn(userId.toString());
        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                authService.refreshAccessToken(refreshToken)
        );
        assertEquals(ErrorCode.AUTH_UNAUTHORIZED, ex.getErrorCode());
    }
} 