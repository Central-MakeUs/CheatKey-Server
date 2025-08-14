package com.cheatkey.module.detection.infra.openai;

import com.cheatkey.module.detection.domain.config.OpenAIConfig;
import com.cheatkey.module.detection.domain.service.DetectionOpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DetectionOpenAiService OpenAI API 통합 테스트")
class DetectionOpenAiServiceIntegrationTest {

    @Autowired
    private DetectionOpenAiService detectionOpenAiService;

    @Autowired
    private OpenAIConfig openAIConfig;

    @Test
    @DisplayName("OpenAI API 호출 성공 테스트")
    void OpenAI_API_호출_성공_테스트() {
        // given
        String prompt = "계좌 정보를 요구하는 의심스러운 문자를 받았습니다. 이것이 피싱인가요?";

        // when
        String response = detectionOpenAiService.generateResponse(prompt);

        // then
        assertNotNull(response);
        assertFalse(response.trim().isEmpty());
    }
}
