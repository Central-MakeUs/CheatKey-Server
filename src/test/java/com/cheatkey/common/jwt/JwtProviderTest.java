package com.cheatkey.common.jwt;

import com.cheatkey.module.auth.domain.entity.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secretKey", "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttest");
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", 1000L * 60 * 10); // 10분
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpirationMs", 1000L * 60 * 60 * 24 * 7); // 7일
    }

    @Test
    void accessToken_정상발급_및_파싱() {
        String token = jwtProvider.createAccessToken(123L, Provider.KAKAO);
        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));
        Claims claims = jwtProvider.getClaimsFromToken(token);
        assertEquals("123", claims.getSubject());
        assertEquals("KAKAO", claims.get("provider"));
    }

    @Test
    void refreshToken_정상발급_및_파싱() {
        String token = jwtProvider.createRefreshToken(456L);
        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));
        Claims claims = jwtProvider.getClaimsFromToken(token);
        assertEquals("456", claims.getSubject());
        // provider claim 없음
        assertNull(claims.get("provider"));
    }

    @Test
    void 만료된_토큰_검증시_false_반환() {
        // 만료시간을 과거로 설정
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", -1000L);
        String token = jwtProvider.createAccessToken(1L, Provider.KAKAO);
        assertFalse(jwtProvider.validateToken(token));
    }

    @Test
    void 잘못된_시크릿_서명오류_예외() {
        String token = jwtProvider.createAccessToken(1L, Provider.KAKAO);
        JwtProvider otherProvider = new JwtProvider();
        ReflectionTestUtils.setField(otherProvider, "secretKey", "wrongwrongwrongwrongwrongwrongwrongwrongwrongwrongwrongwrong");
        ReflectionTestUtils.setField(otherProvider, "accessTokenExpirationMs", 1000L * 60 * 10);
        assertThrows(JwtException.class, () -> otherProvider.getClaimsFromToken(token));
    }

    @Test
    void 시크릿_누락시_예외() {
        JwtProvider noSecretProvider = new JwtProvider();
        ReflectionTestUtils.setField(noSecretProvider, "secretKey", null);
        ReflectionTestUtils.setField(noSecretProvider, "accessTokenExpirationMs", 1000L * 60 * 10);
        assertThrows(NullPointerException.class, () -> noSecretProvider.createAccessToken(1L, Provider.KAKAO));
    }

    @Test
    void 만료시간_누락시_예외() {
        JwtProvider noExpProvider = new JwtProvider();
        ReflectionTestUtils.setField(noExpProvider, "secretKey", "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttest");
        ReflectionTestUtils.setField(noExpProvider, "accessTokenExpirationMs", 0L);
        assertDoesNotThrow(() -> noExpProvider.createAccessToken(1L, Provider.KAKAO)); // 0이면 즉시 만료지만 예외는 아님
    }
} 