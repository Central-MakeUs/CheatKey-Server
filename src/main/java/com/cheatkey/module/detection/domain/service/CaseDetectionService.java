package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.domain.mapper.DetectionMapper;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.interfaces.dto.DetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseDetectionService {
    
    private final DetectionHistoryRepository detectionHistoryRepository;
    private final DetectionMapper detectionMapper;

    // LangGraph 스타일 워크플로우로 교체
    private final LangGraphStyleWorkflow langGraphWorkflow;
    
    @Transactional
    public DetectionResponse detect(DetectionInput input, Long loginUserId) {
        log.info("AI 피싱 사례 분석 시작: userId={}, input={}", loginUserId, input.content());
        
        try {
            // 1. LangGraph 스타일 워크플로우 실행
            DetectionWorkflowState workflowState = langGraphWorkflow.executeWorkflow(input.content());
            
            // 2. 워크플로우 결과 분석
            if (workflowState.getStatus() == DetectionWorkflowState.WorkflowStatus.FAILED) {
                return handleWorkflowFailure(workflowState, input, loginUserId);
            }
            
            // 3. 성공적인 워크플로우 결과 처리
            return handleWorkflowSuccess(workflowState, input, loginUserId);
            
        } catch (Exception e) {
            log.error("AI 피싱 사례 분석 중 예상치 못한 오류 발생", e);
            return handleUnexpectedError(e, input, loginUserId);
        }
    }
    
    /**
     * 워크플로우 실패 처리
     */
    private DetectionResponse handleWorkflowFailure(DetectionWorkflowState workflowState, DetectionInput input, Long loginUserId) {
        log.warn("워크플로우 실패: {}", workflowState.getLastError());
        
        // @TODO UNKNOWN 상태일 때는 히스토리 저장하지 않음
        // INVALID_INPUT_CASE, AMBIGUOUS_INPUT은 피싱과 무관한 입력이므로 이력으로 남길 필요 없음
        // 향후 ActionType 기반 UI 개선 후 정규화 필요
        if (workflowState.getActionType() == ActionType.INVALID_INPUT_CASE || 
            workflowState.getActionType() == ActionType.AMBIGUOUS_INPUT) {
            // UNKNOWN 상태는 히스토리 저장하지 않고 바로 응답 반환
            return createFailureResponse(workflowState);
        }
        
        // 기타 실패 케이스는 히스토리 저장
        DetectionHistory failureHistory = createDetectionHistory(input, loginUserId, DetectionStatus.SAFE, DetectionGroup.PHISHING);
        failureHistory = detectionHistoryRepository.save(failureHistory);
        
//        // 실패한 워크플로우 상태 저장
//        saveWorkflowState(workflowState, failureHistory.getId(), loginUserId);
        
        // 실패 응답 생성
        return createFailureResponse(workflowState);
    }
    
    /**
     * 워크플로우 성공 처리
     */
    private DetectionResponse handleWorkflowSuccess(DetectionWorkflowState workflowState, DetectionInput input, Long loginUserId) {
        log.info("워크플로우 성공: 품질 점수={}, 결과 수={}", 
            workflowState.getQualityAssessment().getOverallScore(),
            workflowState.getResultCount());

        // 1. 검색 결과를 기반으로 위험도 및 카테고리 결정
        DetectionStatus status = workflowState.getDetectionStatus();
        DetectionCategory category = detectionMapper.mapToCategory(workflowState.getSearchResults());
        
        // @TODO 프론트엔드 호환성을 위한 특별 처리
        if (status == null && workflowState.getActionType() != null) {
            if (workflowState.getActionType() == ActionType.INVALID_INPUT_CASE || 
                workflowState.getActionType() == ActionType.AMBIGUOUS_INPUT) {
                status = DetectionStatus.UNKNOWN;
            }
        }
        
        DetectionGroup group;
        if (category != null) {
            group = category.isPhishingGroup() ? DetectionGroup.PHISHING : DetectionGroup.NORMAL;
        } else {
            group = DetectionGroup.PHISHING;
        }
        
        // 2. 성공 이력 저장
        DetectionHistory successHistory = createDetectionHistory(input, loginUserId, status, group);
        successHistory = DetectionHistory.builder()
                .id(successHistory.getId())
                .userId(successHistory.getUserId())
                .inputText(successHistory.getInputText())
                .topScore(workflowState.getPreservedTopScore())
                .status(status)
                .group(group)
                .detectedAt(successHistory.getDetectedAt())
                .detectionType(successHistory.getDetectionType())
                .matchedCaseId(workflowState.getSearchResults().isEmpty() ? null :
                    workflowState.getSearchResults().get(0).id())
                .build();
        detectionHistoryRepository.save(successHistory);
        
//        // 워크플로우 상태 저장
//        saveWorkflowState(workflowState, successHistory.getId(), loginUserId);
        
        // 3. 성공 응답 생성 (품질 평가 결과 포함)
        return new DetectionResponse(
            new DetectionResult(successHistory.getId(), status, group),
            workflowState.getQualityAssessment()
        );
    }
    
    /**
     * 예상치 못한 오류 처리
     */
    private DetectionResponse handleUnexpectedError(Exception e, DetectionInput input, Long loginUserId) {
        log.error("예상치 못한 오류로 인한 분석 실패", e);
        
        // 오류 이력 저장 (기본 정보만)
        DetectionHistory errorHistory = createDetectionHistory(input, loginUserId, null, null);
        detectionHistoryRepository.save(errorHistory);
        
        // 오류 응답 생성
        return createErrorResponse(ActionType.SYSTEM_ERROR, e.getMessage());
    }
    
    /**
     * 검출 이력 생성
     */
    private DetectionHistory createDetectionHistory(DetectionInput input, Long loginUserId, 
                                                  DetectionStatus status, DetectionGroup group) {
        return DetectionHistory.builder()
            .userId(loginUserId)
            .inputText(input.content())
            .detectionType(input.type().name())
            .detectedAt(LocalDateTime.now())
            .status(status != null ? status : DetectionStatus.SAFE)
            .group(group != null ? group : DetectionGroup.PHISHING)
            .build();
    }
    
    /**
     * 실패 응답 생성
     */
    private DetectionResponse createFailureResponse(DetectionWorkflowState workflowState) {
        // workflowState에서 ActionType 가져오기
        ActionType failureActionType = workflowState.getActionType();
        if (failureActionType == null) {
            failureActionType = ActionType.SYSTEM_ERROR; // 기본값
        }
        
        // 기본 품질 평가 생성 (실패 상태)
        QualityAssessment failureQuality = new QualityAssessment();
        failureQuality.setOverallScore(0.0);
        failureQuality.setAcceptable(false);
        failureQuality.setAssessmentTime(LocalDateTime.now());
        failureQuality.setPreservedTopScore(0.0f);
        failureQuality.setSearchAttempts(1);
        failureQuality.setResultCount(0);
        failureQuality.setActionType(failureActionType);

        // 프론트엔드 호환성을 위한 특별 처리
        DetectionStatus failureStatus = DetectionStatus.SAFE; // 기본값
        if (failureActionType == ActionType.INVALID_INPUT_CASE || 
            failureActionType == ActionType.AMBIGUOUS_INPUT) {
            failureStatus = DetectionStatus.UNKNOWN;
        }
        
        // 실패 상태의 DetectionResult 생성
        DetectionResult failureResult = new DetectionResult(
            null,       // ID 없음
            failureStatus, // 프론트엔드 호환성을 위해 적절한 상태 설정
            DetectionGroup.NORMAL // 기본값
        );
        
        return new DetectionResponse(failureResult, failureQuality);
    }
    
    /**
     * 오류 응답 생성
     */
    private DetectionResponse createErrorResponse(ActionType actionType, String errorMessage) {
        // 기본 품질 평가 생성 (오류 상태)
        QualityAssessment errorQuality = new QualityAssessment();
        errorQuality.setOverallScore(0.0);
        errorQuality.setAcceptable(false);
        errorQuality.setAssessmentTime(LocalDateTime.now());
        errorQuality.setPreservedTopScore(0.0f);
        errorQuality.setSearchAttempts(1);
        errorQuality.setResultCount(0);
        errorQuality.setActionType(actionType);

        // @TODO 오류 정의 확인 필요
        // 오류 상태의 DetectionResult 생성
        DetectionResult errorResult = new DetectionResult(
            null,       // ID 없음
            DetectionStatus.SAFE, // 기본값
            DetectionGroup.NORMAL // 기본값
        );
        
        return new DetectionResponse(errorResult, errorQuality);
    }
    
    /**
     * 워크플로우 상태 저장
     * @TODO 워크플로우 히스토리 관리 추가
     */
//    private void saveWorkflowState(DetectionWorkflowState workflowState, Long detectionHistoryId, Long userId) {
//        try {
//            // 사용자 ID와 DetectionHistory ID 설정
//            workflowState.setUserId(userId);
//            workflowState.setDetectionHistoryId(detectionHistoryId);
//
//            // 워크플로우 상태 저장
//            DetectionWorkflowState savedWorkflowState = detectionWorkflowStateRepository.save(workflowState);
//
//            // 품질 평가가 있다면 저장
//            if (workflowState.getQualityAssessment() != null) {
//                QualityAssessment qualityAssessment = workflowState.getQualityAssessment();
//                qualityAssessment.setDetectionWorkflowStateId(savedWorkflowState.getId());
//                qualityAssessmentRepository.save(qualityAssessment);
//            }
//
//            log.info("워크플로우 상태 저장 완료: workflowStateId={}, detectionHistoryId={}",
//                savedWorkflowState.getId(), detectionHistoryId);
//
//        } catch (Exception e) {
//            log.error("워크플로우 상태 저장 중 오류 발생", e);
//            // 워크플로우 상태 저장 실패는 전체 프로세스를 중단하지 않음
//        }
//    }
}
