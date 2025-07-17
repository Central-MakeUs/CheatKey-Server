package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.service.AbstractAuthSignInService;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import org.springframework.stereotype.Service;

@Service
public class KakaoSignInService extends AbstractAuthSignInService {
    private final KakaoIdTokenService kakaoIdTokenService;
    private final KakaoUserInfoService kakaoUserInfoService;
    
    public KakaoSignInService(AuthRepository authRepository, 
                             JwtProvider jwtProvider,
                             KakaoIdTokenService kakaoIdTokenService,
                             KakaoUserInfoService kakaoUserInfoService) {
        super(authRepository, jwtProvider);
        this.kakaoIdTokenService = kakaoIdTokenService;
        this.kakaoUserInfoService = kakaoUserInfoService;
    }
    
    @Override
    protected String validateToken(String idToken) {
        return kakaoIdTokenService.validateToken(idToken);
    }
    
    @Override
    protected Provider getProvider() {
        return Provider.KAKAO;
    }
    
    @Override
    protected String extractEmail(AuthTokenRequest request) {
        return kakaoUserInfoService.fetchEmail(request.getAccessToken());
    }
} 