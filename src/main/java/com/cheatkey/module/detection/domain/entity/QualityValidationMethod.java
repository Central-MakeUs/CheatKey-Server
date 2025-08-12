package com.cheatkey.module.detection.domain.entity;

public enum QualityValidationMethod {
    BACKEND_ONLY("백엔드 규칙만"),
    OPENAI_ANALYSIS("OpenAI 분석"),
    HYBRID_VALIDATION("하이브리드 검증"),
    FALLBACK_RULES("폴백 규칙");
    
    private final String description;
    
    QualityValidationMethod(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
