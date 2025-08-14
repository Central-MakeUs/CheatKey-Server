package com.cheatkey.module.detection.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfig {
    private String apiKey;
    private String model = "gpt-4o-mini"; // 실제 존재하는 모델 사용
    private int maxCompletionTokens = 500;
    private double temperature = 0.1;
}
