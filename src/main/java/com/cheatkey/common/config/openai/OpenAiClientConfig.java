package com.cheatkey.common.config.openai;

import com.cheatkey.module.detection.infra.config.DetectionOpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenAiClientConfig {
    
    private final DetectionOpenAIConfig detectionOpenAIConfig;
    
    @Bean
    public WebClient openAiWebClient() {
        if (!StringUtils.hasText(detectionOpenAIConfig.getApiKey())) {
            log.warn("OpenAI API 키가 설정되지 않았습니다. application.yml에서 openai.api-key를 설정해주세요.");
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. application.yml에서 openai.api-key를 설정해주세요.");
        }
        
        log.info("OpenAI WebClient 초기화 - 모델: {}, 최대 토큰: {}, 온도: {}", 
            detectionOpenAIConfig.getModel(),
            detectionOpenAIConfig.getMaxCompletionTokens(),
            detectionOpenAIConfig.getTemperature());
            
        return WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + detectionOpenAIConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
