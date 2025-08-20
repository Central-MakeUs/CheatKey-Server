package com.cheatkey.module.detection.interfaces;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.detection.interfaces.dto.CaseDetectionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DetectionController 통합 테스트")
@Disabled("외부 API 호출용 테스트 - Vector DB, OpenAI 로컬 세팅 후 수동 실행 전용")
class DetectionControllerIntegrationTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwt;

    @BeforeEach
    void setUp() {
        jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
    }

    @Test
    void 정상적인_AI_분석_사례_검색_통합_테스트() throws Exception {
        // given
        CaseDetectionRequest request = new CaseDetectionRequest(
                "오픈채팅에서 '돈 버는 부업'이라며 소개받은 사이트에 가입했어요. 계좌 정보를 요구합니다."
        );

        // when & then
        mockMvc.perform(post("/v1/api/detection/case")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.group").exists())
                .andExpect(jsonPath("$.detectionId").exists())
                .andExpect(jsonPath("$.qualityScore").exists())
                .andExpect(jsonPath("$.qualityScore").isNumber())
                .andExpect(jsonPath("$.actionType").exists())
                // 1순위 판정 결과 검증 (높은 유사도로 DANGER)
                .andExpect(jsonPath("$.status").value("DANGER"))
                .andExpect(jsonPath("$.actionType").value("IMMEDIATE_ACTION"))
                .andExpect(jsonPath("$.qualityScore").value(8.5));
    }

    @Test
    void 의심스러운_피싱_사례_통합_테스트() throws Exception {
        // given
        CaseDetectionRequest request = new CaseDetectionRequest(
                "[국외발신] 16.7/75 D 누군가 이 외로움을 함께 이겨냈으면 좋겠어요 +T e1efram: https://t.me.hkjp3"
        );

        // when & then
        mockMvc.perform(post("/v1/api/detection/case")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.group").exists())
                .andExpect(jsonPath("$.detectionId").exists())
                .andExpect(jsonPath("$.qualityScore").exists())
                .andExpect(jsonPath("$.actionType").exists())
                // 2순위 판정 결과 검증 (입력 품질 고득점으로 WARNING)
                .andExpect(jsonPath("$.status").value("WARNING"))
                .andExpect(jsonPath("$.actionType").value(anyOf(is("COMMUNITY_SHARE"), is("MANUAL_REVIEW"))));
    }

    @Test
    void 의심스럽지_않은_피싱_사례_통합_테스트() throws Exception {
        // given
        CaseDetectionRequest request = new CaseDetectionRequest(
                "친구들아 우리 함께 이야기 해볼래?"
        );

        // when & then
        mockMvc.perform(post("/v1/api/detection/case")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.group").exists())
                .andExpect(jsonPath("$.actionType").exists());
    }
}
