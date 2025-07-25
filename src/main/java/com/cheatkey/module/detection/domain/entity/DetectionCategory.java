package com.cheatkey.module.detection.domain.entity;

public enum DetectionCategory {
    TRANSACTION(DetectionGroup.NORMAL),     // 거래 사기
    INVESTMENT(DetectionGroup.NORMAL),      // 투자 사기
    PHISHING(DetectionGroup.PHISHING),      // 피싱 사기
    IMPERSONATION(DetectionGroup.PHISHING)  // 사칭 사기
    ;

    private final DetectionGroup group;

    DetectionCategory(DetectionGroup group) {
        this.group = group;
    }

    public DetectionGroup getGroup() {
        return group;
    }

    public boolean isPhishingGroup() {
        return this.group == DetectionGroup.PHISHING;
    }
}
