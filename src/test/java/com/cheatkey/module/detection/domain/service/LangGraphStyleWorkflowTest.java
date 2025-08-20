package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.infra.config.QualityAssessmentConfig;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.domain.service.validation.OpenAIValidationService;
import com.cheatkey.module.detection.domain.service.validation.QualityAssessmentService;
import com.cheatkey.module.detection.domain.service.workflow.LangGraphStyleWorkflow;
import com.cheatkey.module.detection.domain.service.workflow.OpenAICostTracker;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("LangGraphStyleWorkflow 의미 없는 입력 검사 테스트")
class LangGraphStyleWorkflowTest {

    @Mock
    protected QualityAssessmentConfig config;

    @Mock
    protected VectorDbClient vectorDbClient;

    @Mock
    protected QualityAssessmentService qualityAssessmentService;

    @Mock
    protected OpenAIValidationService openAIValidationService;

    @Mock
    protected OpenAICostTracker costTracker;

    @InjectMocks
    protected LangGraphStyleWorkflow workflow;

    @BeforeEach
    void setUp() {
        // 기본 설정
        lenient().when(config.getMaxSearchAttempts()).thenReturn(3);
        lenient().when(config.isEnableOpenAI()).thenReturn(true);
        lenient().when(costTracker.canMakeCall(any(Double.class))).thenReturn(true);
        lenient().when(costTracker.getInputValidationCost()).thenReturn(0.001);
    }

    @Test
    @DisplayName("의미 없는 입력 '똥' 입력 시 즉시 차단")
    void 의미없는_입력_똥_즉시차단() {
        // given
        String userInput = "똥";
        
        // QualityAssessmentService가 의미 없는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(true);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.FAILED);
        assertThat(result.getActionType()).isEqualTo(ActionType.INVALID_INPUT_CASE);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.UNKNOWN);
        assertThat(result.getLastError()).contains("의미 없는 입력으로 판정됨");
    }

    @Test
    @DisplayName("의미 없는 입력 'ㅋㅋㅋ' 입력 시 즉시 차단")
    void 의미없는_입력_반복패턴_즉시차단() {
        // given
        String userInput = "ㅋㅋㅋ";
        
        // QualityAssessmentService가 의미 없는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(true);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.FAILED);
        assertThat(result.getActionType()).isEqualTo(ActionType.INVALID_INPUT_CASE);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.UNKNOWN);
    }

    @Test
    @DisplayName("의미 없는 입력 '안녕하세요' 입력 시 즉시 차단")
    void 의미없는_입력_인사말_즉시차단() {
        // given
        String userInput = "안녕하세요";
        
        // QualityAssessmentService가 의미 없는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(true);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.FAILED);
        assertThat(result.getActionType()).isEqualTo(ActionType.INVALID_INPUT_CASE);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.UNKNOWN);
    }

    @Test
    @DisplayName("의미 있는 입력 '사기' 입력 시 정상 처리")
    void 의미있는_입력_사기_정상처리() {
        // given
        String userInput = "사기";
        
        // QualityAssessmentService가 의미 있는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(false);
        
        // 벡터 검색 결과
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.85f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "사기 사례"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.DANGER);
    }

    @Test
    @DisplayName("의미 있는 입력 '피싱' 입력 시 정상 처리")
    void 의미있는_입력_피싱_정상처리() {
        // given
        String userInput = "피싱";
        
        // QualityAssessmentService가 의미 있는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(false);
        
        // 벡터 검색 결과
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.85f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "피싱 사례"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.DANGER);
    }

    @Test
    @DisplayName("의미 있는 긴 입력 정상 처리")
    void 의미있는_긴입력_정상처리() {
        // given
        String userInput = "오픈채팅에서 부업을 소개받았는데 의심스러워요";
        
        // QualityAssessmentService가 의미 있는 입력으로 판정
        given(qualityAssessmentService.isMeaninglessInput(userInput)).willReturn(false);
        
        // 벡터 검색 결과
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.85f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "부업 소개 피싱"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.DANGER);
    }
}
