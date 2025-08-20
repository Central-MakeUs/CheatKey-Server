package com.cheatkey.module.auth.domain.service.kakao;

import static org.junit.jupiter.api.Assertions.*;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class KakaoSignInServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private KakaoIdTokenService kakaoIdTokenService;
    @Mock private KakaoUserInfoService kakaoUserInfoService;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private KakaoSignInService kakaoSignInService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 신규회원_카카오로그인_성공시_PENDING상태() {
        // given
        String idToken = "mockIdToken";
        String accessToken = "mockAccessToken";
        String providerId = "kakaoId123";
        String email = "test@kakao.com";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .build();

        when(kakaoIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(kakaoUserInfoService.fetchEmail(accessToken)).thenReturn(email);
        when(authRepository.findActiveByProviderAndProviderId(Provider.KAKAO, providerId)).thenReturn(Optional.empty());
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Auth auth = kakaoSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(Provider.KAKAO, auth.getProvider());
        assertEquals(providerId, auth.getProviderId());
        assertEquals(email, auth.getEmail());
        assertEquals(AuthStatus.PENDING, auth.getStatus());
    }

    @Test
    void 기존회원_카카오로그인_성공시_ACTIVE상태_및_로그인카운트증가() {
        // given
        String idToken = "mockIdToken";
        String accessToken = "mockAccessToken";
        String providerId = "kakaoId123";
        String email = "test@kakao.com";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .build();
                
        Auth existingAuth = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId(providerId)
                .email(email)
                .loginCount(1)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        when(kakaoIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(kakaoUserInfoService.fetchEmail(accessToken)).thenReturn(email);
        when(authRepository.findActiveByProviderAndProviderId(Provider.KAKAO, providerId)).thenReturn(Optional.of(existingAuth));
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Auth auth = kakaoSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(AuthStatus.ACTIVE, auth.getStatus());
        assertEquals(2, auth.getLoginCount());
    }

    @Test
    void 카카오idToken_검증실패시_예외발생() {
        // given
        String idToken = "invalidIdToken";
        String accessToken = "mockAccessToken";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .build();
                
        when(kakaoIdTokenService.validateToken(idToken)).thenThrow(new RuntimeException("idToken 검증 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            kakaoSignInService.signIn(request);
        });
        assertTrue(ex.getMessage().contains("idToken 검증 실패"));
    }

    @Test
    void 카카오이메일조회_실패시_예외발생() {
        // given
        String idToken = "mockIdToken";
        String accessToken = "mockAccessToken";
        String providerId = "kakaoId123";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .build();

        when(kakaoIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(kakaoUserInfoService.fetchEmail(accessToken)).thenThrow(new RuntimeException("이메일 조회 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            kakaoSignInService.signIn(request);
        });
        assertTrue(ex.getMessage().contains("이메일 조회 실패"));
    }
}