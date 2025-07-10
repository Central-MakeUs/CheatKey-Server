package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRepository authRepository;

    @AfterEach
    void tearDown() {
        authRepository.deleteByKakaoId(999999999L);
    }

    @Test
    public void 회원가입_후_home_접근시_welcome_true가_반환된다() throws Exception {
        // given
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", 999999999L); // 로그인 세션 설정

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setNickname("테스터");
        request.setAgeCode("30_40");
        request.setGenderCode("FEMALE");
        request.setTradeMethodCodeList(List.of("SNS", "APP"));
        request.setTradeItemCodeList(List.of("FASHION", "LUXURY"));
        request.setAgreedRequiredTerms(List.of(1L, 2L));
        request.setAgreedOptionalTerms(List.of(3L));

        // when: 회원가입 API 호출
        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: /home 진입 시 welcome = true
        mockMvc.perform(get("/api/home").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcome").value(true));

        // 다시 호출하면 welcome = false
        mockMvc.perform(get("/api/home").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcome").value(false));
    }
}
