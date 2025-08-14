package com.cheatkey.module.detection.domain.entity;

public enum DetectionStatus {
    SAFE,      // 안전
    WARNING,   // 경고
    DANGER,    // 위험
    UNKNOWN    // 알 수 없음 (프론트엔드 호환성을 위해 추가)
}
