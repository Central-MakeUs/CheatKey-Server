package com.cheatkey.module.auth.interfaces.controller;

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
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

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
    void 로그인된_상태라면_me_API는_kakaoId를_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginUser").value(999999999L)); // TestLoginMockFilter가 주입한 kakaoId
    }

    @Test
    void 회원가입_초기정보_조회_성공() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", 999999999L);

        mockMvc.perform(get("/api/auth/register").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageCodeList").isArray())
                .andExpect(jsonPath("$.genderCodeList").isArray())
                .andExpect(jsonPath("$.tradeMethodCodeList").isArray())
                .andExpect(jsonPath("$.tradeItemCodeList").isArray())
                .andExpect(jsonPath("$.termsList").isArray());
    }

    @Test
    void 회원가입_성공시_200_반환() throws Exception {
        // given
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", 999999999L);

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setNickname("테스터");
        request.setAgeCode("30_40");
        request.setGenderCode("FEMALE");
        request.setTradeMethodCodeList(List.of("SNS"));
        request.setTradeItemCodeList(List.of("FASHION"));
        request.setAgreedRequiredTerms(List.of(1L, 2L));
        request.setAgreedOptionalTerms(List.of(3L));

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void 로그아웃_성공시_204_반환() throws Exception {
        // given
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", 999999999L);

        // when & then
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
    }
}
