package com.cheatkey.module.detection.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean isValid;
    private String reason;
    private String suggestion;
    private ValidationType validationType;
    private double confidence;
    
    public enum ValidationType {
        VALID_CASE, INVALID_CASE, NEEDS_CLARIFICATION, OPENAI_ERROR
    }
}
