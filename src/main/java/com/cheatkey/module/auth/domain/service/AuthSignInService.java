package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.UserActivity;
import com.cheatkey.module.auth.domain.service.apple.AppleSignInService;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import com.cheatkey.module.auth.domain.service.kakao.KakaoSignInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSignInService {
    private final KakaoSignInService kakaoSignInService;
    private final AppleSignInService appleSignInService;
    private final UserActivityService userActivityService;

    @Transactional
    public Auth signIn(Provider provider, String idToken, String accessToken, String ip, String userAgent) {
        Auth auth = null;
        boolean success = false;
        String failReason = null;

        try {
            if (provider == Provider.KAKAO) {
                AuthTokenRequest request = AuthTokenRequest.builder()
                        .idToken(idToken)
                        .accessToken(accessToken)
                        .build();
                auth = kakaoSignInService.signIn(request);
            } else if (provider == Provider.APPLE) {
                AuthTokenRequest request = AuthTokenRequest.builder()
                        .idToken(idToken)
                        .build();
                auth = appleSignInService.signIn(request);
            } else {
                throw new IllegalArgumentException("Invalid provider");
            }

            success = (auth != null && auth.getStatus() == AuthStatus.ACTIVE);
            return auth;
        } catch (Exception e) {
            failReason = e.getMessage();
            throw e;
        } finally {
            try {
                if (auth != null && (success || auth.getStatus() == AuthStatus.ACTIVE)) {
                    userActivityService.recordActivity(auth.getId(), UserActivity.ActivityType.SOCIAL_LOGIN, ip, userAgent, success, failReason);
                }
            } catch (Exception logEx) {
                log.error("로그인 이력 기록 실패", logEx);
            }
        }
    }
} 