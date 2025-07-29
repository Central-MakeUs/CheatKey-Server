package com.cheatkey.module.home;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.repository.AuthActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeDashboardVisitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private AuthActivityRepository authActivityRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Long testUserId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Auth testUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("test-user-" + System.currentTimeMillis())
                .email("test@test.com")
                .nickname("테스트유저")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        testUser = authRepository.save(testUser);
        testUserId = testUser.getId();
        
        jwtToken = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);
    }

    @AfterEach
    void tearDown() {
        // 테스트용 사용자 삭제
        if (testUserId != null) {
            authRepository.deleteById(testUserId);
        }
    }

    @Test
    void 메인대시보드_방문시_방문횟수_증가() throws Exception {
        // given
        Long initialActivityCount = authActivityRepository.countByUserId(testUserId);
        assertEquals(0L, initialActivityCount);

        // when - 첫 번째 방문
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.totalVisitCount").value(1));

        // then
        Long firstActivityCount = authActivityRepository.countByUserId(testUserId);
        assertEquals(1L, firstActivityCount);

        // when - 두 번째 방문
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.totalVisitCount").value(1)); // 하루에 한 번만 카운팅

        // then
        Long secondActivityCount = authActivityRepository.countByUserId(testUserId);
        assertEquals(2L, secondActivityCount); // 활동 기록은 계속 증가
    }
} 