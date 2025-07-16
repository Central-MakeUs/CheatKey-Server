package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.service.kakao.KakaoSignInService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthSignInServiceTest {
    @Mock private KakaoSignInService kakaoSignInService;
    @Mock private AuthLoginHistoryService authLoginHistoryService;

    @InjectMocks
    private AuthSignInService authSignInService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 카카오로그인_정식회원_로그인히스토리_기록() {
        // given
        Auth auth = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakaoId123")
                .email("test@kakao.com")
                .status(AuthStatus.ACTIVE)
                .build();

        when(kakaoSignInService.signIn(anyString(), anyString())).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(authLoginHistoryService, times(1))
                .recordLogin(eq(auth), eq("127.0.0.1"), eq("UA"), eq(true), isNull());
    }

    @Test
    void 카카오로그인_신규회원_로그인히스토리_기록안함() {
        // given
        Auth auth = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakaoId123")
                .email("test@kakao.com")
                .status(AuthStatus.PENDING)
                .build();

        when(kakaoSignInService.signIn(anyString(), anyString())).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(authLoginHistoryService, never())
                .recordLogin(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void provider가_잘못된경우_예외발생() {
        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authSignInService.signIn(null, "idToken", "accessToken", "127.0.0.1", "UA");
        });
        assertTrue(ex.getMessage().contains("Invalid provider"));
    }

    @Test
    void 카카오로그인_내부서비스_예외발생시_예외전파() {
        // given
        when(kakaoSignInService.signIn(anyString(), anyString()))
                .thenThrow(new RuntimeException("카카오 인증 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");
        });
        assertTrue(ex.getMessage().contains("카카오 인증 실패"));
    }
}