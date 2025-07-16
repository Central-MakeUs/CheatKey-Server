package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.service.apple.AppleSignInService;
import com.cheatkey.module.auth.domain.service.kakao.KakaoSignInService;
import com.cheatkey.module.auth.domain.service.AuthLoginHistoryService;
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
    private final AuthLoginHistoryService authLoginHistoryService;

    @Transactional
    public Auth signIn(Provider provider, String idToken, String accessToken, String ip, String userAgent) {
        Auth auth = null;
        boolean success = false;
        String failReason = null;

        try {
            if (provider == Provider.KAKAO) {
                auth = kakaoSignInService.signIn(idToken, accessToken);
            } else if (provider == Provider.APPLE) {
                // auth = appleSignInService.signIn(idToken);
                throw new UnsupportedOperationException("Apple 로그인은 아직 지원하지 않습니다.");
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
                    authLoginHistoryService.recordLogin(auth, ip, userAgent, success, failReason);
                }
            } catch (Exception logEx) {
                log.error("로그인 이력 기록 실패", logEx);
            }
        }
    }
} 