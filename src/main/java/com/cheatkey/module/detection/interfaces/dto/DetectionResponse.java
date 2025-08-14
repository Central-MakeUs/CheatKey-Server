package com.cheatkey.module.detection.interfaces.dto;

import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "피싱 유사도 분석 결과")
public class DetectionResponse {

    private Long detectionId;

    @Schema(description = """
    검출 상태
    - SAFE: 안전
    - WARNING: 경고
    - DANGER: 위험
    - UNKNOWN: 알 수 없음 (피싱과 무관하거나 맥락이 불분명한 입력)""", 
    example = "SAFE")
    private final DetectionStatus status;

    @Schema(description = """
    사기 그룹
    - NORMAL: 거래&투자 사기
    - PHISHING: 피싱&사칭 사기""", example = "PHISHING")
    private final DetectionGroup group;

    // 새로운 품질 관련 필드 추가
    @Schema(description = "검색 품질 점수 (0-10점)", example = "7.5")
    private Double qualityScore;

    @Schema(description = """
    행동 지침
    - IMMEDIATE_ACTION: 즉시 조치 (DANGER)
    - COMMUNITY_SHARE: 커뮤니티 공유
    - MANUAL_REVIEW: 수동 검토
    - MONITORING: 모니터링
    - NO_ACTION: 조치 불필요
    - INVALID_INPUT_CASE: 피싱과 무관한 입력
    - AMBIGUOUS_INPUT: 맥락이 불분명한 입력""", 
    example = "IMMEDIATE_ACTION")
    private ActionType actionType;

    public DetectionResponse(DetectionResult result) {
        this.detectionId = result.detectionId();
        this.status = result.status();
        this.group = result.group();
    }
    
    // 새로운 생성자 (품질 정보 포함)
    public DetectionResponse(DetectionResult result, QualityAssessment qualityAssessment) {
        this.detectionId = result.detectionId();
        this.status = result.status();
        this.group = result.group();
        
        // 품질 정보 설정
        this.qualityScore = qualityAssessment.getOverallScore();
        this.actionType = qualityAssessment.getActionType();
    }
}
