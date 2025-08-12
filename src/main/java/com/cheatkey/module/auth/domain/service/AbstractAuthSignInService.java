package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.common.jwt.JwtProvider;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public abstract class AbstractAuthSignInService {
    
    protected final AuthRepository authRepository;
    protected final JwtProvider jwtProvider;
    
    @Getter
    protected Auth lastAuth;
    
    protected AbstractAuthSignInService(AuthRepository authRepository, JwtProvider jwtProvider) {
        this.authRepository = authRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public Auth signIn(AuthTokenRequest request) {
        String providerId = validateToken(request.getIdToken());
        Provider provider = getProvider();
        String email = extractEmail(request);

        Optional<Auth> optionalAuth = authRepository.findByProviderAndProviderId(provider, providerId);

        if (optionalAuth.isPresent()) {
            Auth existingAuth = optionalAuth.get();

            // 탈퇴 회원인 경우 재가입 가능 여부 확인
            if (existingAuth.getStatus() == AuthStatus.WITHDRAWN) {
                if (existingAuth.canRejoin()) {
                    // 30일 경과: 신규 회원으로 생성
                    return createNewAuth(provider, providerId, email);
                } else {
                    // 30일 미경과: 재가입 불가
                    throw new CustomException(ErrorCode.AUTH_WITHDRAWN_RECENTLY);
                }
            }

            // 기존 회원인 경우 (ACTIVE, PENDING)
            updateExistingAuth(existingAuth);
            this.lastAuth = existingAuth;
            return existingAuth;
        }

        // 완전 신규 회원
        Auth newAuth = createNewAuth(provider, providerId, email);
        this.lastAuth = newAuth;
        return newAuth;
    }
    
    public SignInResponse toSignInResponse(Auth auth) {
        String accessJwt = jwtProvider.createAccessToken(auth.getId(), auth.getProvider(), auth.getRole());
        String refreshJwt = jwtProvider.createRefreshToken(auth.getId(), auth.getRole());
        return SignInResponse.builder()
                .userState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }
    
    // 추상 메서드 - 각 Provider 별로 구현
    protected abstract String validateToken(String idToken);
    protected abstract Provider getProvider();
    protected abstract String extractEmail(AuthTokenRequest request);
    
    // 공통 로직
    private Auth createNewAuth(Provider provider, String providerId, String email) {
        Auth newAuth = Auth.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .loginCount(1)
                .lastLoginAt(LocalDateTime.now())
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER) // 기본 역할은 USER
                .build();
        return authRepository.save(newAuth);
    }
    
    private void updateExistingAuth(Auth auth) {
        if (auth.getStatus() == AuthStatus.ACTIVE) {
            // 하루에 한 번만 로그인 카운트 증가
            LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
            if (auth.getLastLoginAt() == null || 
                auth.getLastLoginAt().toLocalDate().isBefore(today.toLocalDate())) {
                auth.increaseLoginCount();
            }
            auth.updateLastLoginTime(LocalDateTime.now());
            authRepository.save(auth);
        }
    }
} 