package com.cheatkey.module.auth.domain.service.apple;

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
class AppleSignInServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private AppleIdTokenService appleIdTokenService;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private AppleSignInService appleSignInService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 신규회원_애플로그인_성공시_PENDING상태() {
        // given
        String idToken = "mockIdToken";
        String providerId = "appleId123";
        String email = "test@apple.com";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(authRepository.findByProviderAndProviderId(Provider.APPLE, providerId)).thenReturn(Optional.empty());
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(Provider.APPLE, auth.getProvider());
        assertEquals(providerId, auth.getProviderId());
        assertEquals(AuthStatus.PENDING, auth.getStatus());
    }

    @Test
    void 기존회원_애플로그인_성공시_ACTIVE상태_및_로그인카운트증가() {
        // given
        String idToken = "mockIdToken";
        String providerId = "appleId123";
        String email = "test@apple.com";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();
                
        Auth existingAuth = Auth.builder()
                .provider(Provider.APPLE)
                .providerId(providerId)
                .email(email)
                .loginCount(1)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(authRepository.findByProviderAndProviderId(Provider.APPLE, providerId)).thenReturn(Optional.of(existingAuth));
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(AuthStatus.ACTIVE, auth.getStatus());
        assertEquals(2, auth.getLoginCount());
    }

    @Test
    void 애플idToken_검증실패시_예외발생() {
        // given
        String idToken = "invalidIdToken";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();
                
        when(appleIdTokenService.validateToken(idToken)).thenThrow(new RuntimeException("idToken 검증 실패"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            appleSignInService.signIn(request);
        });
        assertTrue(ex.getMessage().contains("idToken 검증 실패"));
    }

    @Test
    void 애플idToken에서_이메일추출_성공() {
        // given
        String idToken = "validIdTokenWithEmail";
        String providerId = "appleId123";
        
        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);
        when(authRepository.findByProviderAndProviderId(Provider.APPLE, providerId)).thenReturn(Optional.empty());
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(Provider.APPLE, auth.getProvider());
        assertEquals(providerId, auth.getProviderId());
    }
} 