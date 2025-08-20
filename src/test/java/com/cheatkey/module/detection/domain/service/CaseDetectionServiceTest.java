package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.infra.config.QualityAssessmentConfig;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionWorkflowState;
import com.cheatkey.module.detection.domain.entity.ValidationResult;
import com.cheatkey.module.detection.domain.entity.ValidationType;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크플로우 핵심 동작 테스트")
class CaseDetectionServiceTest {

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
        // 기본 설정 - lenient 사용
        lenient().when(config.getMaxSearchAttempts()).thenReturn(3);
        lenient().when(config.isEnableOpenAI()).thenReturn(true);
        lenient().when(costTracker.canMakeCall(anyDouble())).thenReturn(true);
        lenient().when(costTracker.getInputValidationCost()).thenReturn(0.001);
        
        // 입력 품질 점수 Mock 설정 - lenient 사용
        lenient().when(qualityAssessmentService.calculateInputQualityScore(anyString())).thenReturn(7.5);
    }

    @Test
    @DisplayName("유사도 0.7 이상으로 3단계 생략하고 4단계로 진행")
    void 높은_유사도로_3단계_생략() {
        // given
        String userInput = "오픈채팅에서 '돈 버는 부업'이라며 소개받은 사이트에 가입했어요. 계좌 정보를 요구합니다.";

        // 벡터 검색 성공 (높은 유사도)
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.85f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "계좌 정보 요구 피싱"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getWorkflowStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getCurrentStep()).isEqualTo(DetectionWorkflowState.WorkflowStep.RESULT_ANALYSIS);
        assertThat(result.getPreservedTopScore()).isEqualTo(0.85f);
        assertThat(result.getDetectionStatus()).isEqualTo(DetectionStatus.DANGER);
        assertThat(result.isOpenAIUsed()).isFalse(); // OpenAI 사용하지 않음
    }

    @Test
    @DisplayName("유사도 0.5~0.7로 OpenAI 검증 생략하고 4단계로 진행")
    void 중간_유사도로_OpenAI_검증_생략() {
        // given
        String userInput = "이상한 문자 받았는데 의심스러워요";

        // 벡터 검색 성공 (중간 유사도)
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.6f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "의심스러운 문자"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getWorkflowStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getPreservedTopScore()).isEqualTo(0.2f);
        assertThat(result.isOpenAIUsed()).isTrue(); // OpenAI 사용함
    }

    @Test
    @DisplayName("유사도 0.5 미만으로 OpenAI 검증 수행")
    void 낮은_유사도로_OpenAI_검증_수행() {
        // given
        String userInput = "수상한 링크가 왔어요";

        // OpenAI 검증 성공
        ValidationResult openAIValidation = ValidationResult.builder()
                .isValid(true)
                .reason("유효한 피싱 사례")
                .validationType(ValidationType.VALID_CASE)
                .confidence(0.9)
                .build();
        given(openAIValidationService.validateInput(anyString())).willReturn(openAIValidation);

        // 벡터 검색 성공 (낮은 유사도)
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> searchResults = List.of(
                new VectorDbClient.SearchResult("case-1", 0.3f,
                        Map.of("CATEGORY", "피싱", "CONTENT", "수상한 링크"))
        );
        given(vectorDbClient.embed(anyString())).willReturn(embedding);
        given(vectorDbClient.searchSimilarCases(any(), anyInt())).willReturn(searchResults);

        // when
        DetectionWorkflowState result = workflow.executeWorkflow(userInput);

        // then
        assertThat(result.getWorkflowStatus()).isEqualTo(DetectionWorkflowState.WorkflowStatus.COMPLETED);
        assertThat(result.getPreservedTopScore()).isEqualTo(0.1f);
        assertThat(result.isOpenAIUsed()).isTrue(); // OpenAI 사용함
        assertThat(result.getOpenAICallCount()).isEqualTo(1);
    }
}