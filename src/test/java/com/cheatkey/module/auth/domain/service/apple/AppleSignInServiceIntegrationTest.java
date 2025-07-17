package com.cheatkey.module.auth.domain.service.apple;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AppleSignInServiceIntegrationTest {

    @Autowired
    private AppleSignInService appleSignInService;

    @MockBean
    private AppleIdTokenService appleIdTokenService;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        authRepository.deleteAll();
    }

    @Test
    void 신규회원_애플로그인_성공시_PENDING상태로_생성() {
        // given
        String idToken = "mock.apple.id.token";
        String providerId = "apple123";
        String email = "test@apple.com";

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);

        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(Provider.APPLE, auth.getProvider());
        assertEquals(providerId, auth.getProviderId());
        assertEquals(AuthStatus.PENDING, auth.getStatus());
        assertEquals(1, auth.getLoginCount());

        // DB에 실제 저장되었는지 확인
        Optional<Auth> savedAuth = authRepository.findByProviderAndProviderId(Provider.APPLE, providerId);
        assertTrue(savedAuth.isPresent());
        assertEquals(providerId, savedAuth.get().getProviderId());
    }

    @Test
    void 기존회원_애플로그인_성공시_ACTIVE상태_유지_및_로그인카운트증가() {
        // given
        String idToken = "mock.apple.id.token";
        String providerId = "apple123";
        String email = "test@apple.com";

        // 기존 회원 생성
        Auth existingAuth = Auth.builder()
                .provider(Provider.APPLE)
                .providerId(providerId)
                .email(email)
                .loginCount(1)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        authRepository.save(existingAuth);

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);

        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(AuthStatus.ACTIVE, auth.getStatus());
        assertEquals(2, auth.getLoginCount()); // 로그인 카운트 증가
        assertNotNull(auth.getLastLoginAt()); // 마지막 로그인 시간 업데이트
    }

    @Test
    void 애플로그인_성공시_SignInResponse_생성() {
        // given
        String idToken = "mock.apple.id.token";
        String providerId = "apple123";

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);

        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        Auth auth = appleSignInService.signIn(request);

        // when
        SignInResponse response = appleSignInService.toSignInResponse(auth);

        // then
        assertNotNull(response);
        assertEquals(AuthStatus.PENDING.name(), response.getUserState());
        assertEquals("Bearer", response.getGrantType());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void 애플idToken_검증실패시_예외발생() {
        // given
        String invalidIdToken = "invalid.token";

        when(appleIdTokenService.validateToken(invalidIdToken))
                .thenThrow(new RuntimeException("애플 idToken 검증 실패"));

        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(invalidIdToken)
                .build();

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            appleSignInService.signIn(request);
        });
        
        assertTrue(ex.getMessage().contains("애플 idToken 검증 실패"));
    }

    @Test
    void 애플로그인_중복회원_처리() {
        // given
        String idToken = "mock.apple.id.token";
        String providerId = "apple123";
        String email = "test@apple.com";

        // 동일한 providerId로 기존 회원 생성
        Auth existingAuth = Auth.builder()
                .provider(Provider.APPLE)
                .providerId(providerId)
                .email(email)
                .loginCount(1)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        authRepository.save(existingAuth);

        when(appleIdTokenService.validateToken(idToken)).thenReturn(providerId);

        AuthTokenRequest request = AuthTokenRequest.builder()
                .idToken(idToken)
                .build();

        // when
        Auth auth = appleSignInService.signIn(request);

        // then
        assertNotNull(auth);
        assertEquals(existingAuth.getId(), auth.getId()); // 동일한 회원
        assertEquals(2, auth.getLoginCount()); // 로그인 카운트만 증가
    }
} 