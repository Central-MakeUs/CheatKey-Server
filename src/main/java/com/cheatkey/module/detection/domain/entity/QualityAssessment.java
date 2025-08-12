package com.cheatkey.module.detection.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAssessment {
    private double overallScore;
    private QualityGrade qualityGrade;
    private String reason;
    private String suggestion;
    private int searchAttempts;
    private float topSimilarityScore;
    private int resultCount;
    private QualityValidationMethod validationMethod;
    private List<String> improvementSteps;
    private boolean isAcceptable;
    private LocalDateTime assessmentTime;
}
