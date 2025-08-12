package com.cheatkey.module.detection.domain.entity;

public enum DetectionStatus {
    SAFE,      // 안전
    WARNING,   // 경고
    DANGER,    // 위험
    UNKNOWN    // 알 수 없음 (새로운 사례이거나 정보 부족)
}
