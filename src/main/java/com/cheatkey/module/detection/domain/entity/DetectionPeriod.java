package com.cheatkey.module.detection.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum DetectionPeriod {
    
    TODAY("today", "오늘"),
    WEEK("week", "일주일"),
    MONTH("month", "3개월"),
    ALL("all", "전체");
    
    private final String value;
    private final String description;
    
    public static DetectionPeriod fromValue(String value) {
        for (DetectionPeriod period : values()) {
            if (period.value.equals(value)) {
                return period;
            }
        }
        return TODAY; // 기본값
    }
    
    public LocalDateTime getStartDate() {
        LocalDateTime now = LocalDateTime.now();
        return switch (this) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case WEEK -> now.minusWeeks(1);
            case MONTH -> now.minusMonths(3);
            case ALL -> now.minusYears(100); // 전체 기간
        };
    }
} 