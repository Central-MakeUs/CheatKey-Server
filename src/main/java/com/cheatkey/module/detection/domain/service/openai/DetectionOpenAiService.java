package com.cheatkey.module.detection.domain.service.openai;

import com.cheatkey.common.config.openai.dto.OpenAIResponsesRequest;
import com.cheatkey.common.config.openai.dto.OpenAIResponsesResponse;
import com.cheatkey.module.detection.infra.config.DetectionOpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionOpenAiService {
    
    private final DetectionOpenAIConfig detectionOpenAIConfig;
    private final WebClient openAiWebClient;
    
    public String generateResponse(String prompt) {
        try {
            // 실제 OpenAI API 호출
            return callOpenAI(prompt);
            
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            throw new RuntimeException("OpenAI 서비스 일시적 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * OpenAI Chat Completions API 호출
     */
    private String callOpenAI(String prompt) {
        try {
            // 파라미터 유효성 검사
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("프롬프트가 비어있습니다.");
            }
            
            // OpenAIResponsesRequest 생성
            OpenAIResponsesRequest.Message message = OpenAIResponsesRequest.Message.builder()
                .role("user")
                .content(prompt)
                .build();
            
            OpenAIResponsesRequest request = OpenAIResponsesRequest.builder()
                .model(detectionOpenAIConfig.getModel())
                .messages(List.of(message))
                .maxTokens(detectionOpenAIConfig.getMaxCompletionTokens())
                .temperature(detectionOpenAIConfig.getTemperature())
                .build();

            log.debug("OpenAI API 요청 상세: {}", request);
            
            // OpenAI Chat Completions API 호출
            OpenAIResponsesResponse response = openAiWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponsesResponse.class)
                .block();
            
            if (response == null) {
                throw new RuntimeException("OpenAI API 응답이 null입니다.");
            }
            
            // 응답 텍스트 추출
            return extractResponseText(response);
            
        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생", e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 응답 텍스트 추출: choices[0].message.content에서 추출
     */
    private String extractResponseText(OpenAIResponsesResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            var firstChoice = response.getChoices().get(0);
            if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
                String content = firstChoice.getMessage().getContent().trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }
        
        throw new RuntimeException("OpenAI API 응답에서 텍스트를 추출할 수 없습니다.");
    }
}
