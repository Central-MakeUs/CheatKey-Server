package com.cheatkey.module.detection.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 품질 평가 결과에 따른 액션 타입
 */
@Schema(description = "행동 지침")
public enum ActionType {
    @Schema(description = "즉시 조치 (DANGER)")
    IMMEDIATE_ACTION,
    
    @Schema(description = "커뮤니티 공유")
    COMMUNITY_SHARE,
    
    @Schema(description = "수동 검토")
    MANUAL_REVIEW,
    
    @Schema(description = "모니터링")
    MONITORING,
    
    @Schema(description = "조치 불필요")
    NO_ACTION,

    // === 실패/오류 상황 추가 (추적용) ===
    @Schema(description = "워크플로우 실행 실패")
    WORKFLOW_FAILURE,
    
    @Schema(description = "시스템 오류")
    SYSTEM_ERROR,
    
    @Schema(description = "OpenAI API 오류")
    OPENAI_FAILURE,
    
    @Schema(description = "Vector DB 오류")
    VECTOR_DB_FAILURE,
    
    @Schema(description = "시간 초과 오류")
    TIMEOUT_ERROR,
    
    @Schema(description = "입력 개선 오류")
    INPUT_VALIDATION_FAILURE,
    
    @Schema(description = "품질 평가 오류")
    QUALITY_ASSESSMENT_FAILURE,
    
    @Schema(description = "피싱 사례와 무관한 입력")
    INVALID_INPUT_CASE,
    
    @Schema(description = "맥락이 불분명한 입력")
    AMBIGUOUS_INPUT
}
