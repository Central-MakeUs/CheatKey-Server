package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.common.jwt.JwtProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoSignInService {
    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
    private final KakaoIdTokenService kakaoIdTokenService;
    private final KakaoUserInfoService kakaoUserInfoService;

    @Getter
    private Auth lastAuth;

    @Transactional
    public Auth signIn(String idToken, String accessToken) {
        String providerId = kakaoIdTokenService.validateToken(idToken);
        Provider provider = Provider.KAKAO;
        String email = kakaoUserInfoService.fetchEmail(accessToken);

        Optional<Auth> optionalAuth = authRepository.findByProviderAndProviderId(provider, providerId);
        Auth auth = optionalAuth.orElseGet(() -> {
            Auth newAuth = Auth.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .email(email)
                    .loginCount(1)
                    .lastLoginAt(LocalDateTime.now())
                    .status(AuthStatus.PENDING) // 신규 사용자는 PENDING
                    .build();
            return authRepository.save(newAuth);
        });

        // 기존 회원이면 로그인 정보 갱신 및 ACTIVE 상태 유지
        if (auth.getStatus() == AuthStatus.ACTIVE) {
            auth.increaseLoginCount();
            auth.updateLastLoginTime(LocalDateTime.now());
            authRepository.save(auth);
        }
        this.lastAuth = auth;
        return auth;
    }

    public SignInResponse toSignInResponse(Auth auth) {
        String accessJwt = jwtProvider.createAccessToken(auth.getId(), auth.getProvider(), auth.getRole());
        String refreshJwt = jwtProvider.createRefreshToken(auth.getId());
        return SignInResponse.builder()
                .memberState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }
} 