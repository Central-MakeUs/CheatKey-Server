package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.config.QualityAssessmentConfig;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LangGraph 스타일의 상태 기반 워크플로우 엔진
 * - 상태 관리와 조건부 라우팅
 * - 동적 의사결정과 피드백 루프
 * - 멀티 스텝 검증과 개선
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LangGraphStyleWorkflow {
    
    private final QualityAssessmentConfig config;
    private final VectorDbClient vectorDbClient;
    private final QualityAssessmentService qualityAssessmentService;
    private final OpenAIValidationService openAIValidationService;
    private final OpenAICostTracker costTracker;
    
    /**
     * 메인 워크플로우 실행
     */
    public DetectionWorkflowState executeWorkflow(String userInput) {
        // 1. 초기 상태 생성
        DetectionWorkflowState state = DetectionWorkflowState.initialize(userInput, config.getMaxSearchAttempts());
        state.addLog("워크플로우 시작: " + userInput);
        
        try {
            // 2. 단계별 실행
            state = executeStepByStep(state);
            
            // 3. 최종 상태 설정
            if (state.getStatus() != DetectionWorkflowState.WorkflowStatus.FAILED) {
                state.updateStatus(DetectionWorkflowState.WorkflowStatus.COMPLETED);
                state.addLog("워크플로우 성공적으로 완료");
            }
            
        } catch (Exception e) {
            log.error("워크플로우 실행 중 오류 발생", e);
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
            state.setLastError(e.getMessage());
            state.addLog("워크플로우 실패: " + e.getMessage());
        }
        
        return state;
    }
    
    /**
     * 단계별 워크플로우 실행 (LangGraph 스타일)
     */
    private DetectionWorkflowState executeStepByStep(DetectionWorkflowState state) {
        // 1단계: 기본 입력 검증 (입력 개선만 수행)
        state = executeBasicValidation(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 2단계: 백터 검색 (1차)
        state = executeVectorSearch(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 3단계: OpenAI 검증 (조건부)
        if (state.getPreservedTopScore() < 0.5f && state.shouldUseOpenAI() && canUseOpenAI()) {
            state = executeOpenAIValidation(state);
            if (shouldStopWorkflow(state)) return state;
        } else if (state.getPreservedTopScore() < 0.7f && state.getPreservedTopScore() >= 0.5f) {
            state.updateStep(DetectionWorkflowState.WorkflowStep.QUALITY_EVALUATION);
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.QUALITY_ASSESSING);
            state.addLog("3단계 OpenAI 검증 생략: 유사도 0.5 이상으로 4단계로 진행");
        }
        
        // 4단계: 품질 평가
        state = executeQualityAssessment(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 5단계: 결과 분석 및 의사결정
        state = executeResultAnalysis(state);
        
        return state;
    }
    
    /**
     * 1단계: 기본 입력 검증 (입력 개선만 수행)
     */
    private DetectionWorkflowState executeBasicValidation(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.BASIC_VALIDATION)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.INPUT_VALIDATING);
        
        try {
            String improvedInput = improveInputByRules(state.getCurrentInput());
            
            if (!improvedInput.equals(state.getCurrentInput())) {
                state.setCurrentInput(improvedInput);
                state.addLog("입력 텍스트 개선 완료: " + improvedInput);
            } else {
                state.addLog("입력 텍스트 개선 불필요 (이미 최적화됨)");
            }
            
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.SEARCHING);
            
        } catch (Exception e) {
            log.error("입력 개선 중 오류 발생", e);
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
            state.setLastError("입력 개선 실패: " + e.getMessage());
            state.setActionType(ActionType.INPUT_VALIDATION_FAILURE);
        }
        
        return state;
    }
    
    /**
     * 규칙 기반 입력 개선
     */
    private String improveInputByRules(String input) {
        String improved = input.trim();
        
        // 불필요한 공백 정리
        improved = improved.replaceAll("\\s+", " ");
        
        // 특수문자 정리 (URL 등은 유지)
        improved = improved.replaceAll("[\\r\\n\\t]+", " ");
        
        // 앞뒤 공백 제거
        improved = improved.trim();
        
        return improved;
    }
    
    /**
     * 2단계: 백터 검색 (1차)
     */
    private DetectionWorkflowState executeVectorSearch(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.VECTOR_SEARCH)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.SEARCHING);
        
        try {
            // 임베딩 생성
            List<Float> embedding = vectorDbClient.embed(state.getCurrentInput());
            state.addLog("임베딩 생성 완료");
            
            // 벡터 검색
            List<VectorDbClient.SearchResult> results = vectorDbClient.searchSimilarCases(embedding, 5);
            state.setSearchResults(results);
            state.setResultCount(results.size());
            
            if (!results.isEmpty()) {
                float topScore = results.get(0).score();
                state.setPreservedTopScore(topScore); // 백터 검색 점수 보존
                state.addLog("백터 검색 완료: " + results.size() + "개 결과, 최고 점수: " + topScore);
                
                if (topScore >= 0.7f) {
                    state.addLog("높은 유사도(0.7 이상)로 4단계 품질 평가로 진행");
                    // 높은 유사도일 때 상태 업데이트
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.QUALITY_ASSESSING);
                } else {
                    state.addLog("낮은 유사도(0.7 미만)로 3단계 OpenAI 검증으로 진행");
                    // 낮은 유사도일 때 상태 업데이트
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.SEARCHING);
                }
            } else {
                state.setPreservedTopScore(0.0f); // 검색 결과 없음
                state.addLog("백터 검색 결과 없음");
            }
            
        } catch (Exception e) {
            log.error("백터 검색 중 오류 발생", e);
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
            state.setLastError("백터 검색 실패: " + e.getMessage());
            state.setActionType(ActionType.VECTOR_DB_FAILURE);
        }
        
        return state;
    }

    /**
     * 3단계: OpenAI 검증
     */
    private DetectionWorkflowState executeOpenAIValidation(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.OPENAI_VALIDATION);
        state.updateStatus(DetectionWorkflowState.WorkflowStatus.INPUT_VALIDATING);

        try {
            // 비용 추적
            double estimatedCost = costTracker.getInputValidationCost();
            costTracker.incrementCallCount();
            costTracker.addCost(estimatedCost);

            ValidationResult validation = openAIValidationService.validateInput(state.getCurrentInput());
            state.setInputValidation(validation);
            state.setOpenAIUsed(true);
            state.setOpenAICallCount(state.getOpenAICallCount() + 1);
            state.setEstimatedCost(state.getEstimatedCost() + estimatedCost);
            state.setOpenaiConfidence(validation.getConfidence()); // OpenAI 신뢰도 저장

            if (!validation.isValid()) {
                state.addLog("OpenAI 검증 실패: " + validation.getReason());
                if (validation.getValidationType() == ValidationType.INVALID_CASE) {
                    // 피싱 사례와 무관한 입력 (인사말, 수학 문제 등)
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
                    state.setDecisionReason(DetectionWorkflowState.DecisionReason.INPUT_TOO_VAGUE);
                    state.setActionType(ActionType.INVALID_INPUT_CASE);
                    state.addLog("피싱 사례와 무관한 입력으로 판정: " + validation.getReason());
                } else if (validation.getValidationType() == ValidationType.NEEDS_CLARIFICATION) {
                    // 맥락이 불분명한 입력
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
                    state.setDecisionReason(DetectionWorkflowState.DecisionReason.INPUT_TOO_VAGUE);
                    state.setActionType(ActionType.AMBIGUOUS_INPUT);
                    state.addLog("입력 맥락이 불분명하여 추가 설명 필요: " + validation.getReason());
                } else {
                    // 기타 검증 실패
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
                    state.setDecisionReason(DetectionWorkflowState.DecisionReason.INPUT_TOO_VAGUE);
                    state.setActionType(ActionType.INPUT_VALIDATION_FAILURE);
                    state.addLog("OpenAI 검증 실패로 워크플로우 중단: " + validation.getReason());
                }
            } else {
                state.addLog("OpenAI 검증 통과");
                // OpenAI 검증 완료 후 4단계로 진행할 상태 업데이트
                state.updateStatus(DetectionWorkflowState.WorkflowStatus.QUALITY_ASSESSING);
            }

        } catch (Exception e) {
            log.warn("OpenAI 검증 중 오류 발생", e);
            state.addLog("OpenAI 검증 오류: " + e.getMessage());
            state.setActionType(ActionType.OPENAI_FAILURE);
        }

        return state;
    }

    /**
     * 4단계: 품질 평가
     */
    private DetectionWorkflowState executeQualityAssessment(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.QUALITY_EVALUATION)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.QUALITY_ASSESSING);
        
        try {
            float preservedTopScore = state.getPreservedTopScore();
            
            // 1순위: 백터 검색 결과 (topScore >= 0.7)
            if (preservedTopScore >= 0.7f) {
                QualityAssessment quality = createDangerAssessment(preservedTopScore, state.getSearchResults().size());
                state.setQualityAssessment(quality);
                state.addLog("1순위: 높은 유사도로 DANGER 판정 - 점수: " + preservedTopScore);
                // 1순위 완료 후 5단계로 진행할 상태 업데이트
                state.updateStatus(DetectionWorkflowState.WorkflowStatus.DECISION_MAKING);
                return state;
            }
            
            // 2순위: 입력 품질 기반 판정 (고득점일 때)
            if (preservedTopScore < 0.7f) {
                // 입력 품질 점수 계산
                double finalInputQualityScore = getFinalInputQualityScore(state.getCurrentInput(), state.getOpenaiConfidence());
                
                // 고득점 기준: 6.0 이상
                if (finalInputQualityScore >= 6.0) {
                    QualityAssessment quality = assessByInputQuality(preservedTopScore, state.getSearchResults().size(), finalInputQualityScore);
                    state.setQualityAssessment(quality);
                    state.addLog("2순위: OpenAI Confidence 반영 입력 품질 고득점(" + finalInputQualityScore + ")으로 판정 완료");
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.DECISION_MAKING);
                    return state;
                }
            }
            
            // 3순위: 입력 품질 기반 판정 (저득점일 때)
            double finalInputQualityScore = getFinalInputQualityScore(state.getCurrentInput(), state.getOpenaiConfidence());
            QualityAssessment quality = assessByLowInputQuality(preservedTopScore, state.getSearchResults().size(), finalInputQualityScore);
            state.setQualityAssessment(quality);
            
            // 품질 평가 완료 후 5단계로 진행할 상태 업데이트
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.DECISION_MAKING);
            
        } catch (Exception e) {
            log.error("품질 평가 중 오류 발생", e);
            state.addLog("품질 평가 실패: " + e.getMessage());
            state.setActionType(ActionType.QUALITY_ASSESSMENT_FAILURE);
        }
        
        return state;
    }
    
    /**
     * 1순위: 높은 유사도로 DANGER 판정
     */
    private QualityAssessment createDangerAssessment(float topScore, int resultCount) {
        return QualityAssessment.builder()
            .overallScore(8.5) // @TODO 높은 유사도로 위험 판정 : 하드 코딩 재확인
            .searchAttempts(1)
            .preservedTopScore(topScore)
            .resultCount(resultCount)
            .improvementSteps(List.of("Vector DB 검색 완료"))
            .isAcceptable(true)
            .assessmentTime(LocalDateTime.now())
            .actionType(ActionType.IMMEDIATE_ACTION)
            .build();
    }
    
    /**
     * 2순위: 입력 품질 기반 판정 (고득점일 때)
     */
    private QualityAssessment assessByInputQuality(float topScore, int resultCount, double inputQualityScore) {
        if (topScore < 0.3f) {
            // 입력 품질 높음 + 유사도 낮음 (0.3 미만) → 새로운 패턴
            return QualityAssessment.builder()
                .overallScore(inputQualityScore)
                .searchAttempts(1)
                .preservedTopScore(topScore)
                .resultCount(resultCount)
                .improvementSteps(List.of("2순위: 입력 품질 고득점 + 유사도 낮음"))
                .isAcceptable(true)
                .assessmentTime(LocalDateTime.now())
                .actionType(ActionType.COMMUNITY_SHARE)
                .build();
                
        } else if (topScore >= 0.3f && topScore < 0.7f) {
            // 입력 품질 높음 + 유사도 중간 (0.3~0.7) → 주의 필요
            return QualityAssessment.builder()
                .overallScore(inputQualityScore)
                .searchAttempts(1)
                .preservedTopScore(topScore)
                .resultCount(resultCount)
                .improvementSteps(List.of("2순위: 입력 품질 고득점 + 유사도 중간"))
                .isAcceptable(true)
                .assessmentTime(LocalDateTime.now())
                .actionType(ActionType.MANUAL_REVIEW)
                .build();
        }
        
        // 예상치 못한 케이스
        throw new IllegalStateException("예상치 못한 케이스: topScore=" + topScore);
    }
    
    /**
     * 3순위: 입력 품질 기반 판정 (저득점일 때)
     */
    private QualityAssessment assessByLowInputQuality(float topScore, int resultCount, double inputQualityScore) {
        if (topScore < 0.3f) {
            // 입력 품질 저득점 + 유사도 낮음 (0.3 미만) → SAFE (NO_ACTION)
            QualityAssessment assessment = QualityAssessment.builder()
                .overallScore(inputQualityScore)
                .searchAttempts(1)
                .preservedTopScore(topScore)
                .resultCount(resultCount)
                .improvementSteps(List.of("3순위: 입력 품질 저득점 + 유사도 낮음"))
                .isAcceptable(false) // 신뢰할 수 없음
                .assessmentTime(LocalDateTime.now())
                .actionType(ActionType.NO_ACTION) // 조치 불필요
                .build();
            
            log.info("3순위: 입력 품질 저득점({}) + 유사도 0.3 이하로 SAFE 상태 판정", inputQualityScore);
            return assessment;
        } else if (topScore >= 0.3f && topScore < 0.7f) {
            // 입력 품질 저득점 + 유사도 중간 (0.3~0.7) → WARNING (MANUAL_REVIEW)
            QualityAssessment assessment = QualityAssessment.builder()
                .overallScore(inputQualityScore)
                .searchAttempts(1)
                .preservedTopScore(topScore)
                .resultCount(resultCount)
                .improvementSteps(List.of("3순위: 입력 품질 저득점 + 유사도 중간"))
                .isAcceptable(true) // 부분적 신뢰 가능
                .assessmentTime(LocalDateTime.now())
                .actionType(ActionType.MANUAL_REVIEW) // 수동 검토 필요
                .build();
            
            log.info("3순위: 입력 품질 저득점({}) + 유사도 0.3~0.7로 WARNING 상태 판정", inputQualityScore);
            return assessment;
        }
        
        // 예상치 못한 케이스
        throw new IllegalStateException("예상치 못한 케이스: topScore=" + topScore);
    }
    
    /**
     * 5단계: 결과 분석 및 의사결정
     */
    private DetectionWorkflowState executeResultAnalysis(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.RESULT_ANALYSIS)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.DECISION_MAKING);
        
        QualityAssessment quality = state.getQualityAssessment();
        float preservedTopScore = state.getPreservedTopScore();
        
        // 최종 판정
        DetectionStatus finalStatus = determineFinalStatus(quality, preservedTopScore);
        DetectionWorkflowState.DecisionReason decisionReason = determineDecisionReason(quality, preservedTopScore);
        ActionType nextAction = quality.getActionType();

        // 최종 상태 설정
        state.setDetectionStatus(finalStatus);
        state.setDecisionReason(decisionReason);
        state.setNextAction(nextAction);

        state.addLog(String.format("최종 판정 완료: %s", finalStatus));
        return state;
    }
    
    /**
     * 최종 상태 결정
     */
    private DetectionStatus determineFinalStatus(QualityAssessment quality, float preservedTopScore) {
        switch (quality.getActionType()) {
            case IMMEDIATE_ACTION:
                return DetectionStatus.DANGER;
            case COMMUNITY_SHARE:
                return DetectionStatus.WARNING;
            case MANUAL_REVIEW:
                return DetectionStatus.WARNING;
            case MONITORING:
                return DetectionStatus.SAFE;
            case NO_ACTION:
                return DetectionStatus.SAFE;
            default:
                // fallback: 기존 로직
                if (preservedTopScore >= 0.7f) {
                    return DetectionStatus.DANGER;
                } else if (preservedTopScore >= 0.3f) {
                    return DetectionStatus.WARNING;
                } else {
                    return DetectionStatus.SAFE;
                }
        }
    }
    
    /**
     * 의사결정 사유 결정
     */
    private DetectionWorkflowState.DecisionReason determineDecisionReason(QualityAssessment quality, float preservedTopScore) {
        switch (quality.getActionType()) {
            case IMMEDIATE_ACTION:
                return DetectionWorkflowState.DecisionReason.HIGH_SIMILARITY;
            case COMMUNITY_SHARE:
                return DetectionWorkflowState.DecisionReason.COMMUNITY_SHARE_SUGGESTED;
            case MANUAL_REVIEW:
                return DetectionWorkflowState.DecisionReason.MIXED_SIGNALS;
            case MONITORING:
                return DetectionWorkflowState.DecisionReason.LOW_RISK_PATTERN;
            case NO_ACTION:
                return DetectionWorkflowState.DecisionReason.LOW_RISK_PATTERN;
            default:
                // fallback: 기존 로직
                if (preservedTopScore >= 0.7f) {
                    return DetectionWorkflowState.DecisionReason.HIGH_SIMILARITY;
                } else if (preservedTopScore >= 0.3f) {
                    return DetectionWorkflowState.DecisionReason.MEDIUM_QUALITY_RESULTS;
                } else {
                    return DetectionWorkflowState.DecisionReason.LOW_QUALITY_RESULTS;
                }
        }
    }
    
    /**
     * 워크플로우 중단 여부 확인
     */
    private boolean shouldStopWorkflow(DetectionWorkflowState state) {
        return state.getStatus() == DetectionWorkflowState.WorkflowStatus.FAILED ||
               state.getStatus() == DetectionWorkflowState.WorkflowStatus.NEEDS_HUMAN_INTERVENTION;
    }
    
    /**
     * OpenAI 사용 가능 여부 확인
     */
    private boolean canUseOpenAI() {
        return config.isEnableOpenAI() && 
               costTracker.canMakeCall(costTracker.getInputValidationCost());
    }

    /**
     * 입력 품질 점수 계산 (10점 만점)
     */
    private double getInputQualityScore(String input) {
        return qualityAssessmentService.calculateInputQualityScore(input);
    }
    
    /**
     * OpenAI Confidence를 반영한 최종 입력 품질 점수 계산
     * 70:30 가중치 적용 (우리 정책 70%, OpenAI Confidence 30%)
     */
    private double getFinalInputQualityScore(String input, double openaiConfidence) {
        // 기본 입력 품질 점수 (우리 정책 기반)
        double ourPolicyScore = qualityAssessmentService.calculateInputQualityScore(input);
        
        // OpenAI Confidence 점수 (0.0~1.0 → 0.0~10.0)
        double openaiScore = openaiConfidence * 10.0;

        return (ourPolicyScore * 0.7) + (openaiScore * 0.3);
    }
}
