package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.service.AuthSignInService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthSignInService authSignInService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("정상 소셜 로그인 요청시 JWT가 발급된다")
    void socialLogin_success() throws Exception {
        Auth mockAuth = Auth.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("mockProviderId")
                .email("test@kakao.com")
                .status(AuthStatus.ACTIVE)
                .build();

        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(mockAuth);
        given(jwtProvider.createAccessToken(anyLong(), any())).willReturn("mockAccessTokenJwt");
        given(jwtProvider.createRefreshToken(anyLong())).willReturn("mockRefreshTokenJwt");

        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockAccessTokenJwt"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshTokenJwt"))
                .andExpect(jsonPath("$.memberState").value("ACTIVE"));
    }

    @Test
    @DisplayName("provider가 잘못된 경우 400 반환")
    void socialLogin_invalidProvider() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("provider", "INVALID");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("idToken 누락시 400 반환")
    void socialLogin_missingIdToken() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        // idToken 누락
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("서비스에서 인증 실패시 401 반환")
    void socialLogin_authServiceFail() throws Exception {
        // 인증 실패 상황: 서비스가 null 반환
        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(null);

        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
} 