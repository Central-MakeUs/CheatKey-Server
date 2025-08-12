package com.cheatkey.module.detection.domain.config;

import com.theokanning.openai.service.OpenAiService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfig {
    private String apiKey;
    private String model = "gpt-5o-nano";
    private int maxTokens = 500;
    private double temperature = 0.1;
    
    @Bean
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. application.yml에서 openai.api-key를 설정해주세요.");
        }
        return new OpenAiService(apiKey);
    }
}
