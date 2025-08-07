package com.cheatkey.module.mypage.interfaces.dto;

import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "분석 내역 상세 응답")
public class DetectionDetailResponse {
    
    @Schema(description = "분석 내역 ID", example = "1")
    private Long id;
    
    @Schema(description = "분석 상태", example = "SAFE")
    private DetectionStatus status;
    
    @Schema(description = "분석 타입", example = "URL")
    private DetectionType detectionType;
    
    @Schema(description = "입력 텍스트", example = "https://example.com")
    private String inputText;
    
    @Schema(description = "유사도 점수 (CASE 타입인 경우)", example = "0.85")
    private Float topScore;
    
    @Schema(description = "매칭된 사례 ID (CASE 타입인 경우)", example = "case_123")
    private String matchedCaseId;
    
    @Schema(description = "분석 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime detectedAt;
    
    @Schema(description = """
    사기 그룹
    - NORMAL: 거래&투자 사기
    - PHISHING: 피싱&사칭 사기""", example = "PHISHING")
    private DetectionGroup group;
} 