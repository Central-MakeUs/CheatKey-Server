package com.cheatkey.module.detection.domain.entity;

import com.cheatkey.module.detection.infra.client.VectorDbClient;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;

/**
 * LangGraph 스타일의 상태 기반 워크플로우 상태 관리
 */
@Data
@Table(name = "t_detection_workflow_state")
@Builder
@Accessors(chain = true)
public class DetectionWorkflowState {
    
    // 기본 정보
    private String originalInput;
    private String currentInput;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    
    // 워크플로우 상태
    private WorkflowStatus status;
    private WorkflowStep currentStep;
    private int attemptCount;
    private int maxAttempts;
    
    // 검증 결과
    private ValidationResult inputValidation;
    private QualityAssessment qualityAssessment;
    private List<String> improvementSuggestions;
    
    // 검색 결과
    private List<VectorDbClient.SearchResult> searchResults;
    private float preservedTopScore; // 백터 검색 점수 보존 (2단계에서 설정)
    private int resultCount;
    
    // OpenAI 사용 현황
    private boolean openAIUsed;
    private int openAICallCount;
    private double estimatedCost;
    private double openaiConfidence; // OpenAI 검증 신뢰도 추가
    
    // 에러 및 로그
    private List<String> workflowLog;
    private String lastError;
    
    // 의사결정 정보
    private DecisionReason decisionReason;
    private ActionType nextAction;
    private DetectionStatus detectionStatus;  // 최종 판단 결과 (SAFE, WARNING, DANGER, UNKNOWN)
    
    // 위험도 및 공유 정보
    private String estimatedRisk;            // 예상 위험도 (LOW, MEDIUM, HIGH)
    private boolean shouldShareCommunity;    // 커뮤니티 공유 권장 여부
    private List<String> communityCategories; // 추천 커뮤니티 카테고리
    private String communityShareMessage;    // 커뮤니티 공유 안내 메시지
    private String communityShareTitle;      // 커뮤니티 공유 제목 제안
    private ActionType actionType;           // 액션 타입 (추적용)
    
    public enum WorkflowStatus {
        INITIALIZED,           // 초기화됨
        INPUT_VALIDATING,      // 입력 검증 중
        SEARCHING,             // 검색 중
        QUALITY_ASSESSING,     // 품질 평가 중
        DECISION_MAKING,       // 의사결정 중
        COMPLETED,             // 완료됨
        FAILED,                // 실패함
        NEEDS_HUMAN_INTERVENTION // 인간 개입 필요
    }
    
    public enum WorkflowStep {
        BASIC_VALIDATION,      // 기본 검증
        OPENAI_VALIDATION,    // OpenAI 검증
        VECTOR_SEARCH,        // 벡터 검색
        QUALITY_EVALUATION,   // 품질 평가
        RESULT_ANALYSIS,      // 결과 분석
        FINAL_DECISION        // 최종 의사결정
    }
    
    public enum DecisionReason {
        HIGH_QUALITY_RESULTS,     // 높은 품질 결과
        MEDIUM_QUALITY_RESULTS,   // 중간 품질 결과
        LOW_QUALITY_RESULTS,      // 낮은 품질 결과
        HIGH_SIMILARITY,          // 높은 유사도
        NO_RESULTS,               // 결과 없음
        INPUT_TOO_VAGUE,          // 입력이 너무 모호함
        COST_LIMIT_REACHED,       // 비용 제한 도달
        OPENAI_FAILURE,           // OpenAI 실패
        MANUAL_INTERVENTION_NEEDED, // 수동 개입 필요
        COMMUNITY_SHARE_SUGGESTED,  // 커뮤니티 공유 제안
        MIXED_SIGNALS,              // 혼재된 신호
        LOW_RISK_PATTERN            // 낮은 위험 패턴
    }
    
    // 상태 업데이트 메서드들
    public DetectionWorkflowState updateStep(WorkflowStep step) {
        this.currentStep = step;
        this.lastUpdateTime = LocalDateTime.now();
        this.workflowLog.add(String.format("[%s] %s 단계로 이동", 
            LocalDateTime.now(), step));
        return this;
    }
    
    public DetectionWorkflowState updateStatus(WorkflowStatus status) {
        this.status = status;
        this.lastUpdateTime = LocalDateTime.now();
        this.workflowLog.add(String.format("[%s] 상태 변경: %s", 
            LocalDateTime.now(), status));
        return this;
    }
    
    public DetectionWorkflowState addLog(String message) {
        this.workflowLog.add(String.format("[%s] %s", 
            LocalDateTime.now(), message));
        return this;
    }
    
    public boolean shouldUseOpenAI() {
        return !this.openAIUsed && this.attemptCount < 2;
    }
    
    // 초기 상태 생성
    public static DetectionWorkflowState initialize(String input, int maxAttempts) {
        return DetectionWorkflowState.builder()
            .originalInput(input)
            .currentInput(input)
            .startTime(LocalDateTime.now())
            .lastUpdateTime(LocalDateTime.now())
            .status(WorkflowStatus.INITIALIZED)
            .currentStep(WorkflowStep.BASIC_VALIDATION)
            .attemptCount(0)
            .maxAttempts(maxAttempts)
            .workflowLog(new ArrayList<>())
            .improvementSuggestions(new ArrayList<>())
            .searchResults(new ArrayList<>())
            .preservedTopScore(0.0f) // 백터 검색 점수 초기화
            .resultCount(0) // 검색 결과 수 초기화
            .openAIUsed(false) // OpenAI 사용 여부 초기화
            .openAICallCount(0) // OpenAI 호출 횟수 초기화
            .estimatedCost(0.0) // 예상 비용 초기화
            .openaiConfidence(0.0) // OpenAI 신뢰도 초기화
            .inputValidation(null) // 입력 검증 결과 초기화
            .qualityAssessment(null) // 품질 평가 결과 초기화
            .lastError(null) // 마지막 오류 초기화
            .decisionReason(null) // 의사결정 사유 초기화
            .nextAction(null) // 다음 액션 초기화
            .detectionStatus(null) // 검출 상태 초기화
            .estimatedRisk(null) // 예상 위험도 초기화
            .shouldShareCommunity(false) // 커뮤니티 공유 권장 여부 초기화
            .communityCategories(new ArrayList<>()) // 커뮤니티 카테고리 초기화
            .communityShareMessage(null) // 커뮤니티 공유 메시지 초기화
            .communityShareTitle(null) // 커뮤니티 공유 제목 초기화
            .actionType(null) // 액션 타입 초기화
            .build();
    }
}
