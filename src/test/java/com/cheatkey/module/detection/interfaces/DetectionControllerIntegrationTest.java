package com.cheatkey.module.detection.interfaces;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Disabled("Vector DB 실 API 호출용 테스트 - Vector DB 로컬 세팅 후 수동 실행 전용")
class DetectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private DetectionHistoryRepository detectionHistoryRepository;

    private Long testHistoryId;
    private String jwt;

    @BeforeEach
    void setUp() {
        jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        
        // 테스트용 히스토리 데이터 생성
        DetectionHistory history = DetectionHistory.builder()
                .inputText("테스트 입력 텍스트")
                .status(DetectionStatus.SAFE)
                .group(DetectionGroup.PHISHING)
                .detectionType(DetectionType.CASE.name())
                .userId(1L)
                .topScore(0.5f)
                .matchedCaseId("test_case_123")
                .detectedAt(LocalDateTime.now())
                .build();
        
        DetectionHistory savedHistory = detectionHistoryRepository.save(history);
        testHistoryId = savedHistory.getId();
    }

    @Test
    void 검색시_유사_사례_내용이_포함된다() throws Exception {
        // given
        String inputText = "오픈채팅에서 '돈 버는 부업'이라며 소개받은 사이트에 가입했어요.";
        String requestJson = objectMapper.writeValueAsString(Map.of("text", inputText));

        // when & then
        mockMvc.perform(post("/v1/api/detection/case")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.group").value(DetectionGroup.PHISHING.name()))
                .andExpect(jsonPath("$.detectionId").isNumber());
    }

    @Test
    void 분석_결과_상세_조회_성공() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/detection/history/" + testHistoryId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testHistoryId))
                .andExpect(jsonPath("$.status").value(DetectionStatus.SAFE.name()))
                .andExpect(jsonPath("$.detectionType").value(DetectionType.CASE.name()))
                .andExpect(jsonPath("$.inputText").value("테스트 입력 텍스트"))
                .andExpect(jsonPath("$.topScore").value(0.5))
                .andExpect(jsonPath("$.matchedCaseId").value("test_case_123"))
                .andExpect(jsonPath("$.detectedAt").exists())
                .andExpect(jsonPath("$.group").value(DetectionGroup.PHISHING.name()));
    }

    @Test
    void 분석_결과_상세_조회_실패_존재하지_않는_ID() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/detection/history/99999")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 분석_결과_상세_조회_실패_다른_사용자_데이터() throws Exception {
        // given: 다른 사용자의 JWT
        String otherUserJwt = jwtProvider.createAccessToken(2L, Provider.KAKAO, AuthRole.USER);

        // when & then
        mockMvc.perform(get("/v1/api/detection/history/" + testHistoryId)
                        .header("Authorization", "Bearer " + otherUserJwt))
                .andExpect(status().isBadRequest());
    }
} 