package com.cheatkey.module.detection.domain.entity;

public enum QualityGrade {
    EXCELLENT(9.0, "우수"),
    GOOD(7.0, "양호"),
    ACCEPTABLE(5.0, "수용 가능"),
    POOR(3.0, "부족"),
    UNACCEPTABLE(1.0, "수용 불가");
    
    private final double minScore;
    private final String description;
    
    QualityGrade(double minScore, String description) {
        this.minScore = minScore;
        this.description = description;
    }
    
    public static QualityGrade fromScore(double score) {
        for (QualityGrade grade : values()) {
            if (score >= grade.minScore) {
                return grade;
            }
        }
        return UNACCEPTABLE;
    }
    
    public double getMinScore() {
        return minScore;
    }
    
    public String getDescription() {
        return description;
    }
}
