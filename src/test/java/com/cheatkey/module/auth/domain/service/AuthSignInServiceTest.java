package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.entity.UserActivity;
import com.cheatkey.module.auth.domain.service.kakao.KakaoSignInService;
import com.cheatkey.module.auth.domain.service.apple.AppleSignInService;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AuthSignInServiceTest {
    @Mock private KakaoSignInService kakaoSignInService;
    @Mock private AppleSignInService appleSignInService;
    @Mock private UserActivityService userActivityService;

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
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("kakaoId123")
                .email("test@kakao.com")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        when(kakaoSignInService.signIn(any(AuthTokenRequest.class))).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(userActivityService, times(1))
                .recordActivity(eq(1L), eq(UserActivity.ActivityType.SOCIAL_LOGIN), eq("127.0.0.1"), eq("UA"), eq(true), isNull());
    }

    @Test
    void 카카오로그인_신규회원_로그인히스토리_기록안함() {
        // given
        Auth auth = Auth.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("kakaoId123")
                .email("test@kakao.com")
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        when(kakaoSignInService.signIn(any(AuthTokenRequest.class))).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(userActivityService, never())
                .recordActivity(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void 애플로그인_정식회원_로그인히스토리_기록() {
        // given
        Auth auth = Auth.builder()
                .id(1L)
                .provider(Provider.APPLE)
                .providerId("appleId123")
                .email("test@apple.com")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        when(appleSignInService.signIn(any(AuthTokenRequest.class))).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.APPLE, "idToken", null, "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(userActivityService, times(1))
                .recordActivity(eq(1L), eq(UserActivity.ActivityType.SOCIAL_LOGIN), eq("127.0.0.1"), eq("UA"), eq(true), isNull());
    }

    @Test
    void 애플로그인_신규회원_로그인히스토리_기록안함() {
        // given
        Auth auth = Auth.builder()
                .id(1L)
                .provider(Provider.APPLE)
                .providerId("appleId123")
                .email("test@apple.com")
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        when(appleSignInService.signIn(any(AuthTokenRequest.class))).thenReturn(auth);

        // when
        Auth result = authSignInService.signIn(Provider.APPLE, "idToken", null, "127.0.0.1", "UA");

        // then
        assertNotNull(result);
        verify(userActivityService, never())
                .recordActivity(any(), any(), any(), any(), anyBoolean(), any());
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
        when(kakaoSignInService.signIn(any(AuthTokenRequest.class)))
                .thenThrow(new RuntimeException("카카오 인증 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authSignInService.signIn(Provider.KAKAO, "idToken", "accessToken", "127.0.0.1", "UA");
        });
        assertTrue(ex.getMessage().contains("카카오 인증 실패"));
    }

    @Test
    void 애플로그인_내부서비스_예외발생시_예외전파() {
        // given
        when(appleSignInService.signIn(any(AuthTokenRequest.class)))
                .thenThrow(new RuntimeException("애플 인증 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authSignInService.signIn(Provider.APPLE, "idToken", null, "127.0.0.1", "UA");
        });
        assertTrue(ex.getMessage().contains("애플 인증 실패"));
    }
}