package com.cheatkey.module.detection.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "quality.assessment")
@Data
public class QualityAssessmentConfig {
    
    private double minAcceptableScore = 5.0;        // 최소 수용 가능 점수
    private int maxSearchAttempts = 2;              // 최대 검색 시도 횟수
    private boolean enableOpenAI = true;            // OpenAI 사용 여부
    private double openAICostLimit = 0.01;          // OpenAI 비용 제한 (달러)
    private int maxOpenAICallsPerDay = 100;         // 일일 최대 OpenAI 호출 횟수
    
    // 입력 품질 점수 계산을 위한 길이 설정
    private int minLength = 10;                     // 최소 길이 (0.1점)
    private int minAcceptableLength = 20;           // 최소 수용 가능 길이 (0.3점)
    private int minGoodLength = 30;                 // 최소 좋은 길이 (0.5점)
}
