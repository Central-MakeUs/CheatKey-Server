package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.kakao.KakaoAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class KakaoCallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KakaoAuthService kakaoAuthService;

    @MockBean
    private AuthRepository authRepository;

    @Test
    void 신규_사용자라면_signup으로_redirect된다() throws Exception {
        Long kakaoId = 88888L;

        given(kakaoAuthService.handleKakaoLogin(any(), any())).willReturn(kakaoId);
        given(authRepository.findByKakaoId(kakaoId)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/login/kakao/callback")
                        .param("code", "dummy-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/signup"));
    }

    @Test
    void 기존_사용자라면_home으로_redirect된다() throws Exception {
        Long kakaoId = 88888L;

        given(kakaoAuthService.handleKakaoLogin(any(), any())).willReturn(kakaoId);

        Auth dummyUser = Auth.builder()
                .id(1L)
                .kakaoId(88888L)
                .nickname("테스터")
                .ageCode("30_40")
                .genderCode("FEMALE")
                .tradeMethodCode("SNS,APP")
                .tradeItemCode("FASHION,LUXURY")
                .loginCount(1)
                .lastLoginAt(LocalDateTime.now())
                .build();

        given(authRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(dummyUser));

        mockMvc.perform(get("/api/auth/login/kakao/callback")
                        .param("code", "dummy-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/home"));
    }
}

