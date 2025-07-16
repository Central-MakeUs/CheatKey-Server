package com.cheatkey.module.auth.domain.service.kakao;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Service
public class KakaoIdTokenService {
    private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";
    private final WebClient webClient = WebClient.create();

    public String validateToken(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            String kid = signedJWT.getHeader().getKeyID();

            // 1. 카카오 OIDC 공개키 JWKS 가져오기
            String jwksJson = webClient.get()
                    .uri(KAKAO_JWKS_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JWKSet jwkSet = JWKSet.parse(jwksJson);
            List<JWK> keys = jwkSet.getKeys();
            JWK jwk = keys.stream()
                    .filter(k -> k.getKeyID().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No matching JWK found for kid: " + kid));

            // 2. 공개키로 서명 검증
            RSAKey rsaKey = (RSAKey) jwk;
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            if (!signedJWT.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(publicKey))) {
                throw new RuntimeException("Invalid idToken signature");
            }

            // 3. 만료 등 클레임 검증 (필요시 추가)
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 4. 카카오ID(subject) 반환
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("카카오 idToken 검증 실패: " + e.getMessage(), e);
        }
    }
} 