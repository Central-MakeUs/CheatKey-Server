package com.cheatkey.module.detection.domain.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.cheatkey.module.detection.domain.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionOpenAiService {
    
    private final OpenAIConfig openAIConfig;
    private final OpenAiService openAiService;
    
    public String generateResponse(String prompt) {
        try {
            log.info("GPT-5-nano API 호출 시작: {} 토큰", prompt.length());
            
            // OpenAI API 설정 정보 로깅
            log.debug("모델: {}, 최대 토큰: {}, 온도: {}", 
                openAIConfig.getModel(), 
                openAIConfig.getMaxTokens(), 
                openAIConfig.getTemperature());
            
            // 실제 OpenAI API 호출
            return callOpenAI(prompt);
            
        } catch (Exception e) {
            log.error("GPT-5-nano API 호출 실패", e);
            throw new RuntimeException("GPT-5-nano 서비스 일시적 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * 실제 OpenAI API 호출
     */
    private String callOpenAI(String prompt) {
        try {
            // ChatCompletionRequest 생성
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(openAIConfig.getMaxTokens())
                .temperature(openAIConfig.getTemperature())
                .build();
            
            // OpenAI API 호출
            String response = openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
            
            log.info("GPT-5-nano API 호출 성공: {} 토큰 응답", response.length());
            return response;
            
        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생", e);
            throw new RuntimeException("OpenAI API 호출 실패", e);
        }
    }
}
