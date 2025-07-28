package com.cheatkey.module.mypage.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.mypage.interfaces.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    private static final Long TEST_USER_ID = 1L;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = jwtProvider.createAccessToken(TEST_USER_ID, Provider.KAKAO, AuthRole.USER);
    }

    @Test
    @DisplayName("마이페이지 대시보드 조회 API 테스트")
    void 마이페이지_대시보드_조회_API_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/mypage/dashboard")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo").exists())
                .andExpect(jsonPath("$.profileImages").exists());
    }

    @Test
    @DisplayName("사용자 정보 수정 API 테스트")
    void 사용자_정보_수정_API_테스트() throws Exception {
        // given
        UpdateUserInfoRequest request = new UpdateUserInfoRequest();
        request.setNickname("새닉네임");
        request.setProfileImageId(1L);

        // when & then
        mockMvc.perform(put("/v1/api/mypage/userInfo")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("작성글 관리 조회 API 테스트")
    void 작성글_관리_조회_API_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/mypage/community/posts/management")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").exists())
                .andExpect(jsonPath("$.totalPosts").exists())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    @DisplayName("분석 내역 조회 API 테스트")
    void 분석_내역_조회_API_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/mypage/detection/history")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("period", "all")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("페이징 파라미터 기본값 테스트")
    void 페이징_파라미터_기본값_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/mypage/community/posts/management")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));
    }
} 