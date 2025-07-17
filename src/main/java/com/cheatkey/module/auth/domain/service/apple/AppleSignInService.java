package com.cheatkey.module.auth.domain.service.apple;

import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.service.AbstractAuthSignInService;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import org.springframework.stereotype.Service;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class AppleSignInService extends AbstractAuthSignInService {
    private final AppleIdTokenService appleIdTokenService;
    
    public AppleSignInService(AuthRepository authRepository, 
                             JwtProvider jwtProvider,
                             AppleIdTokenService appleIdTokenService) {
        super(authRepository, jwtProvider);
        this.appleIdTokenService = appleIdTokenService;
    }
    
    @Override
    protected String validateToken(String idToken) {
        return appleIdTokenService.validateToken(idToken);
    }
    
    @Override
    protected Provider getProvider() {
        return Provider.APPLE;
    }
    
    @Override
    protected String extractEmail(AuthTokenRequest request) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.getIdToken());
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getStringClaim("email");
        } catch (Exception e) {
            return null;
        }
    }
}