package com.cheatkey.module.detection.domain.entity;

/**
 * 품질 평가 결과에 따른 액션 타입
 */
public enum ActionType {
    IMMEDIATE_ACTION,           // 즉시 조치 (DANGER)
    COMMUNITY_SHARE,            // 커뮤니티 공유
    MANUAL_REVIEW,              // 수동 검토
    MONITORING,                 // 모니터링
    NO_ACTION,                  // 조치 불필요

    // === 실패/오류 상황 추가 (추적용) ===
    WORKFLOW_FAILURE,           // 워크플로우 실행 실패
    SYSTEM_ERROR,               // 시스템 오류
    OPENAI_FAILURE,             // OpenAI API 오류
    VECTOR_DB_FAILURE,          // Vector DB 오류
    TIMEOUT_ERROR,              // 시간 초과 오류
    INPUT_VALIDATION_FAILURE,   // 입력 개선 오류
    QUALITY_ASSESSMENT_FAILURE, // 품질 평가 오류
    INVALID_INPUT_CASE,         // 피싱 사례와 무관한 입력
    AMBIGUOUS_INPUT             // 맥락이 불분명한 입력
}
