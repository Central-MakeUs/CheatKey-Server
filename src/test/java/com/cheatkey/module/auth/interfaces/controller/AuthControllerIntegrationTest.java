package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
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

    @MockBean
    private AuthRepository authRepository;

    @Test
    @DisplayName("정상 소셜 로그인 요청시 JWT가 발급된다")
    void socialLogin_success() throws Exception {
        Auth mockAuth = Auth.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("mockProviderId")
                .email("test@kakao.com")
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(mockAuth);
        given(jwtProvider.createAccessToken(anyLong(), any(), any())).willReturn("mockAccessTokenJwt");
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
                .andExpect(jsonPath("$.userState").value(AuthStatus.PENDING.name()));
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
    @DisplayName("ACTIVE 상태가 아닌 유저는 보호 API 접근 시 401/403 반환")
    void nonActiveUser_accessProtectedApi_forbidden() throws Exception {
        // GIVEN: WITHDRAWN 상태의 유저
        Long userId = 100L;
        Auth withdrawnAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.WITHDRAWN)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(withdrawnAuth));

        // WHEN & THEN: 보호 API(예: /logout) 접근 시 401/403
        mockMvc.perform(post("/v1/api/auth/logout")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"mockRefreshToken\"}"))
                .andExpect(status().isUnauthorized()); // 또는 isForbidden()
    }

    @Test
    @DisplayName("ACTIVE 상태 유저는 보호 API 정상 접근 가능")
    void activeUser_accessProtectedApi_success() throws Exception {
        Long userId = 101L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));

        mockMvc.perform(post("/v1/api/auth/logout")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"mockRefreshToken\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인/회원가입 등 @SkipUserStatusCheck 적용 API는 상태와 무관하게 정상 동작")
    void skipUserStatusCheckApi_worksRegardlessOfStatus() throws Exception {
        // GIVEN: WITHDRAWN 상태의 유저
        Long userId = 102L;
        Auth withdrawnAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.WITHDRAWN)
                .role(AuthRole.USER)
                .build();

        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(withdrawnAuth);
        given(jwtProvider.createAccessToken(anyLong(), any(), any())).willReturn("mockAccessTokenJwt");
        given(jwtProvider.createRefreshToken(anyLong())).willReturn("mockRefreshTokenJwt");

        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        // WHEN & THEN: 로그인은 상태와 무관하게 정상 동작
        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockAccessTokenJwt"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshTokenJwt"));
    }
} 