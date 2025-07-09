package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
@SpringBootTest
@WithMockUser
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRepository authRepository;

    @Test
    public void 회원가입_후_home_접근시_welcome_true가_반환된다() throws Exception {
        // given
        Long kakaoId = 999999L;

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setNickname("테스터");
        request.setAgeCode("30_40");
        request.setGenderCode("FEMALE");
        request.setTradeMethodCodeList(List.of("SNS", "APP"));
        request.setTradeItemCodeList(List.of("FASHION", "LUXURY"));

        // 운영 약관 ID 기준 하드코딩 (변경 주의)
        request.setAgreedRequiredTerms(List.of(1L, 2L));
        request.setAgreedOptionalTerms(List.of(3L));

        // mock OAuth2User 세션 주입
        OAuth2User mockUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("kakaoId", kakaoId),
                "kakaoId"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();

        // when: 회원가입 API 호출
        mockMvc.perform(post("/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: /home 진입 시 welcome = true
        mockMvc.perform(get("/home").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcome").value(true));

        // 다시 호출하면 welcome = false (세션에서 제거됨)
        mockMvc.perform(get("/home").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcome").value(false));
    }
}
