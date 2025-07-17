package com.cheatkey.module.auth.domain.service.util;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

public class JwtValidationUtil {
    private static final WebClient webClient = WebClient.create();

    public static String validateToken(String idToken, String jwksUrl) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            String kid = signedJWT.getHeader().getKeyID();

            // 1. OIDC 공개키 JWKS 가져오기
            String jwksJson = webClient.get()
                    .uri(jwksUrl)
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

            // 4. 사용자 ID(subject) 반환
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("토큰 검증 실패: " + e.getMessage(), e);
        }
    }
} 