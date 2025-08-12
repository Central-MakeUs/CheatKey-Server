package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.config.QualityAssessmentConfig;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        // 1단계: 기본 입력 검증
        state = executeBasicValidation(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 2단계: OpenAI 검증 (조건부)
        if (state.shouldUseOpenAI() && canUseOpenAI()) {
            state = executeOpenAIValidation(state);
            if (shouldStopWorkflow(state)) return state;
        }
        
        // 3단계: 질문 개선 (조건부)
        if (needsQueryImprovement(state)) {
            state = executeQueryImprovement(state);
            if (shouldStopWorkflow(state)) return state;
        }
        
        // 4단계: 벡터 검색
        state = executeVectorSearch(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 5단계: 품질 평가
        state = executeQualityAssessment(state);
        if (shouldStopWorkflow(state)) return state;
        
        // 6단계: 결과 분석 및 의사결정
        state = executeResultAnalysis(state);
        
        return state;
    }
    
    /**
     * 1단계: 기본 입력 검증
     */
    private DetectionWorkflowState executeBasicValidation(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.BASIC_VALIDATION)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.INPUT_VALIDATING);
        
        // 규칙 기반 기본 검증
        ValidationResult validation = performBasicValidation(state.getCurrentInput());
        state.setInputValidation(validation);
        
        if (!validation.isValid()) {
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.INPUT_TOO_VAGUE);
            state.addLog("기본 검증 실패: " + validation.getReason());
        } else {
            state.addLog("기본 검증 통과");
        }
        
        return state;
    }
    
    /**
     * 2단계: OpenAI 검증
     */
    private DetectionWorkflowState executeOpenAIValidation(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.OPENAI_VALIDATION);
        
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
            
            if (!validation.isValid()) {
                state.addLog("OpenAI 검증 실패: " + validation.getReason());
                if (validation.getValidationType() == ValidationResult.ValidationType.INVALID_CASE) {
                    state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
                    state.setDecisionReason(DetectionWorkflowState.DecisionReason.INPUT_TOO_VAGUE);
                }
            } else {
                state.addLog("OpenAI 검증 통과");
            }
            
        } catch (Exception e) {
            log.warn("OpenAI 검증 중 오류 발생", e);
            state.addLog("OpenAI 검증 오류: " + e.getMessage());
            // OpenAI 실패 시 기본 검증 결과 사용
        }
        
        return state;
    }
    
    /**
     * 3단계: 질문 개선
     */
    private DetectionWorkflowState executeQueryImprovement(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.QUERY_IMPROVEMENT)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.INPUT_IMPROVING);
        
        try {
            // 비용 추적
            double estimatedCost = costTracker.getQueryImprovementCost();
            costTracker.incrementCallCount();
            costTracker.addCost(estimatedCost);
            
            String improvedQuery = openAIValidationService.improveQuery(
                state.getCurrentInput(), 
                state.getQualityAssessment()
            );
            
            state.setCurrentInput(improvedQuery);
            state.setOpenAICallCount(state.getOpenAICallCount() + 1);
            state.setEstimatedCost(state.getEstimatedCost() + estimatedCost);
            state.addLog("질문 개선 완료: " + improvedQuery);
            
        } catch (Exception e) {
            log.warn("질문 개선 중 오류 발생", e);
            state.addLog("질문 개선 실패, 원본 입력 사용: " + e.getMessage());
            // 개선 실패 시 원본 입력 사용
        }
        
        return state;
    }
    
    /**
     * 4단계: 벡터 검색
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
                state.setTopSimilarityScore(results.get(0).score());
                state.addLog("벡터 검색 완료: " + results.size() + "개 결과, 최고 점수: " + results.get(0).score());
            } else {
                state.addLog("벡터 검색 결과 없음");
            }
            
        } catch (Exception e) {
            log.error("벡터 검색 중 오류 발생", e);
            state.updateStatus(DetectionWorkflowState.WorkflowStatus.FAILED);
            state.setLastError("벡터 검색 실패: " + e.getMessage());
            state.addLog("벡터 검색 실패: " + e.getMessage());
        }
        
        return state;
    }
    
    /**
     * 5단계: 품질 평가
     */
    private DetectionWorkflowState executeQualityAssessment(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.QUALITY_EVALUATION)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.QUALITY_ASSESSING);
        
        try {
            QualityAssessment quality = qualityAssessmentService.assessQuality(
                state.getSearchResults(), 
                state.getCurrentInput()
            );
            
            state.setQualityAssessment(quality);
            state.addLog("품질 평가 완료: " + quality.getOverallScore() + "/10점");
            
        } catch (Exception e) {
            log.error("품질 평가 중 오류 발생", e);
            state.addLog("품질 평가 실패: " + e.getMessage());
        }
        
        return state;
    }
    
    /**
     * 6단계: 결과 분석 및 의사결정
     */
    private DetectionWorkflowState executeResultAnalysis(DetectionWorkflowState state) {
        state.updateStep(DetectionWorkflowState.WorkflowStep.RESULT_ANALYSIS)
             .updateStatus(DetectionWorkflowState.WorkflowStatus.DECISION_MAKING);
        
        // 품질 점수 기반 의사결정
        double qualityScore = state.getQualityAssessment().getOverallScore();
        
        // Vector DB 검색 결과가 없는 경우 특별 처리
        if (state.getSearchResults().isEmpty()) {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.NO_RESULTS);
            state.setNextAction("유사한 피싱 사례를 찾을 수 없습니다. 새로운 사례로 등록하여 향후 참고할 수 있습니다.");
            state.setDetectionStatus(DetectionStatus.UNKNOWN);  // 새로운 상태 설정
            
            // 위험도 및 커뮤니티 공유 정보 설정
            state.setEstimatedRisk(calculateEstimatedRisk(0.0, 0.0f, state.getCurrentInput()));
            state.setShouldShareCommunity(true);
            state.setCommunityCategories(List.of("REPORT", "SHARE"));
            state.setCommunityShareTitle("새로운 피싱 사례 신고");
            state.setCommunityShareMessage("이 상황을 커뮤니티에 공유하여 다른 사용자들에게 도움을 주세요. " +
                "새로운 유형의 피싱 수법일 수 있어 즉시 공유가 권장됩니다.");
            
            state.addLog("검색 결과 없음: 새로운 사례로 등록 권장");
            return state;
        }
        
        // 최고 유사도 점수가 너무 낮은 경우 특별 처리
        float topScore = state.getTopSimilarityScore();
        if (topScore < 0.3f) {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.LOW_QUALITY_RESULTS);
            state.setNextAction("유사한 피싱 사례를 찾을 수 없습니다. 더 구체적인 상황을 설명해주시거나 새로운 사례로 등록해주세요.");
            state.setDetectionStatus(DetectionStatus.UNKNOWN);  // 새로운 상태 설정
            
            // 위험도 및 커뮤니티 공유 정보 설정
            state.setEstimatedRisk(calculateEstimatedRisk(3.0, topScore, state.getCurrentInput()));
            state.setShouldShareCommunity(true);
            state.setCommunityCategories(List.of("REPORT", "SHARE"));
            state.setCommunityShareTitle("의심스러운 피싱 사례 공유");
            state.setCommunityShareMessage("유사한 사례가 없어 정확한 분석이 어렵습니다. " +
                "커뮤니티에 공유하여 다른 사용자들과 정보를 교환해보세요.");
            
            state.addLog(String.format("유사도 점수 낮음 (%.2f): 새로운 사례로 등록 권장", topScore));
            return state;
        }
        
        // 일반적인 품질 점수 기반 의사결정
        if (qualityScore >= 8.0) {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.HIGH_QUALITY_RESULTS);
            state.setNextAction("높은 품질 결과로 신뢰할 수 있음");
        } else if (qualityScore >= 6.0) {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.HIGH_QUALITY_RESULTS);
            state.setNextAction("양호한 품질 결과로 참고 가능");
        } else if (qualityScore >= 4.0) {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.LOW_QUALITY_RESULTS);
            state.setNextAction("제한적인 품질로 추가 정보 필요");
        } else {
            state.setDecisionReason(DetectionWorkflowState.DecisionReason.LOW_QUALITY_RESULTS);
            state.setNextAction("낮은 품질로 재검색 또는 사용자 안내 필요");
        }
        
        state.addLog(String.format("의사결정 완료: %s (품질 점수: %.1f)", state.getNextAction(), qualityScore));
        return state;
    }
    
    /**
     * 기본 검증 수행
     */
    private ValidationResult performBasicValidation(String input) {
        // 간단한 규칙 기반 검증
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("입력이 비어있습니다")
                .suggestion("구체적인 상황을 설명해주세요")
                .validationType(ValidationResult.ValidationType.INVALID_CASE)
                .confidence(0.0)
                .build();
        }
        
        if (input.length() < 5) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("입력이 너무 짧습니다")
                .suggestion("더 구체적인 상황을 설명해주세요")
                .validationType(ValidationResult.ValidationType.NEEDS_CLARIFICATION)
                .confidence(0.3)
                .build();
        }
        
        return ValidationResult.builder()
            .isValid(true)
            .reason("기본 검증 통과")
            .validationType(ValidationResult.ValidationType.VALID_CASE)
            .confidence(0.7)
            .build();
    }
    
    /**
     * 워크플로우 중단 여부 확인
     */
    private boolean shouldStopWorkflow(DetectionWorkflowState state) {
        return state.getStatus() == DetectionWorkflowState.WorkflowStatus.FAILED ||
               state.getStatus() == DetectionWorkflowState.WorkflowStatus.NEEDS_HUMAN_INTERVENTION;
    }
    
    /**
     * 질문 개선 필요 여부 확인
     */
    private boolean needsQueryImprovement(DetectionWorkflowState state) {
        if (state.getQualityAssessment() == null) return false;
        
        double score = state.getQualityAssessment().getOverallScore();
        return score < 5.0 && state.shouldUseOpenAI();
    }
    
    /**
     * OpenAI 사용 가능 여부 확인
     */
    private boolean canUseOpenAI() {
        return config.isEnableOpenAI() && 
               costTracker.canMakeCall(costTracker.getInputValidationCost());
    }
    
    /**
     * 예상 위험도 계산
     */
    private String calculateEstimatedRisk(double qualityScore, float topScore, String userInput) {
        // 1. 품질 점수 기반 기본 위험도
        String baseRisk = mapQualityScoreToRisk(qualityScore);
        
        // 2. 유사도 점수 보정
        if (topScore < 0.3f) {
            return "HIGH";  // 유사도 낮음 = 새로운 사례로 높은 위험
        }
        
        // 3. 입력 내용 기반 위험도
        String contentRisk = analyzeInputContentRisk(userInput);
        
        // 4. 최종 위험도 결정
        return determineFinalRisk(baseRisk, contentRisk);
    }
    
    /**
     * 품질 점수를 위험도로 매핑
     */
    private String mapQualityScoreToRisk(double score) {
        if (score >= 8.0) return "LOW";      // 높은 품질 = 낮은 위험
        if (score >= 6.0) return "MEDIUM";   // 양호한 품질 = 중간 위험
        if (score >= 4.0) return "HIGH";     // 제한적 품질 = 높은 위험
        return "HIGH";                        // 낮은 품질 = 높은 위험
    }
    
    /**
     * 입력 내용 기반 위험도 분석
     */
    private String analyzeInputContentRisk(String input) {
        String lowerInput = input.toLowerCase();
        
        // 높은 위험 키워드
        if (lowerInput.contains("계좌") || lowerInput.contains("비밀번호") || 
            lowerInput.contains("송금") || lowerInput.contains("이체")) {
            return "HIGH";
        }
        
        // 중간 위험 키워드
        if (lowerInput.contains("링크") || lowerInput.contains("클릭") || 
            lowerInput.contains("문자") || lowerInput.contains("전화")) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
    
    /**
     * 최종 위험도 결정
     */
    private String determineFinalRisk(String baseRisk, String contentRisk) {
        // HIGH가 하나라도 있으면 HIGH
        if ("HIGH".equals(baseRisk) || "HIGH".equals(contentRisk)) {
            return "HIGH";
        }
        
        // MEDIUM이 하나라도 있으면 MEDIUM
        if ("MEDIUM".equals(baseRisk) || "MEDIUM".equals(contentRisk)) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
}
