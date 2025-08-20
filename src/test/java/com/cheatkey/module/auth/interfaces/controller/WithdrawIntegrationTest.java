package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.token.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WithdrawIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private AuthRepository authRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("회원 탈퇴 성공")
    @WithMockUser(username = "104", authorities = "ROLE_USER")
    void withdraw_success() throws Exception {
        // GIVEN
        Long userId = 104L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        
        // Mock JWT Provider
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        
        // Mock Repository
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        
        // Mock Services
        doNothing().when(authService).withdrawUser(userId, "WITHDRAWAL_REASON_006");
        doNothing().when(refreshTokenService).invalidateTokenByUserId(userId);

        // WHEN & THEN
        mockMvc.perform(delete("/v1/api/auth/withdraw")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"WITHDRAWAL_REASON_006\"}"))
                .andExpect(status().isOk());

        // Verify service methods were called
        verify(authService).withdrawUser(userId, "WITHDRAWAL_REASON_006");
        verify(refreshTokenService).invalidateTokenByUserId(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - reasonCode 누락시 기본값 사용")
    @WithMockUser(username = "106", authorities = "ROLE_USER")
    void withdraw_success_withDefaultReasonCode() throws Exception {
        // GIVEN
        Long userId = 106L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        
        // Mock JWT Provider
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        
        // Mock Repository
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        
        // Mock Services
        doNothing().when(authService).withdrawUser(userId, "WITHDRAWAL_REASON_006");
        doNothing().when(refreshTokenService).invalidateTokenByUserId(userId);
        
        // WHEN & THEN - reasonCode 필드 누락시 기본값 사용
        mockMvc.perform(delete("/v1/api/auth/withdraw")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
        
        // Verify service methods were called with default value
        verify(authService).withdrawUser(userId, "WITHDRAWAL_REASON_006");
        verify(refreshTokenService).invalidateTokenByUserId(userId);
    }
}
