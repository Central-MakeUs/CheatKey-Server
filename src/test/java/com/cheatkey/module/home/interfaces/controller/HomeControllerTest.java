package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.home.application.facade.HomeFacade;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
// import com.fasterxml.jackson.databind.ObjectMapper; // 미사용
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeFacade homeFacade;

    @Autowired
    private JwtProvider jwtProvider;

    private static final Long TEST_USER_ID = 1L;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = jwtProvider.createAccessToken(TEST_USER_ID, Provider.KAKAO, AuthRole.USER);
    }

    @Test
    @DisplayName("메인 대시보드 조회 성공")
    void getDashboard_Success() throws Exception {
        // given
        HomeDashboardResponse.UserInfo userInfo = HomeDashboardResponse.UserInfo.builder()
                .profileImageUrl("https://cdn.../profile.jpg")
                .level(1)
                .nickname("닉네임")
                .totalVisitCount(5)
                .build();

        HomeDashboardResponse.PopularPost popularPost = HomeDashboardResponse.PopularPost.builder()
                .id(1L)
                .title("택배 지연 문자 조심!!")
                .content("택배 지연 문자를 클릭하자 악성 앱이 설치되어 개인 정보가 유출됐어요...")
                .authorNickname("사기꾼")
                .build();

        HomeDashboardResponse response = HomeDashboardResponse.builder()
                .userInfo(userInfo)
                .popularPosts(List.of(popularPost))
                .build();

        when(homeFacade.getDashboard(eq(TEST_USER_ID))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.profileImageUrl").value("https://cdn.../profile.jpg"))
                .andExpect(jsonPath("$.userInfo.level").value(1))
                .andExpect(jsonPath("$.userInfo.nickname").value("닉네임"))
                .andExpect(jsonPath("$.userInfo.totalVisitCount").value(5))
                .andExpect(jsonPath("$.popularPosts[0].id").value(1))
                .andExpect(jsonPath("$.popularPosts[0].title").value("택배 지연 문자 조심!!"))
                .andExpect(jsonPath("$.popularPosts[0].content").value("택배 지연 문자를 클릭하자 악성 앱이 설치되어 개인 정보가 유출됐어요..."))
                .andExpect(jsonPath("$.popularPosts[0].authorNickname").value("사기꾼"));
    }


} 