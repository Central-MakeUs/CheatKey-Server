package com.cheatkey.module.detection.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "검출 상태")
public enum DetectionStatus {
    @Schema(description = "안전")
    SAFE,
    
    @Schema(description = "경고")
    WARNING,
    
    @Schema(description = "위험")
    DANGER,
    
    @Schema(description = "알 수 없음 (피싱과 무관하거나 맥락이 불분명한 입력)")
    UNKNOWN
}
