package com.cheatkey.module.detection.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.domain.mapper.DetectionMapper;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CaseDetectionServiceTest {

    @InjectMocks
    private CaseDetectionService caseDetectionService;

    @Mock
    private VectorDbClient vectorDbClient;

    @Mock
    private DetectionMapper detectionMapper;

    @Mock
    private DetectionHistoryRepository historyRepository;

    @Test
    public void 입력_받으면_유사도_검색하고_결과를_반환한다() {
        // given
        Long kakaoId = 1L;

        String inputText = "의심스러운 문자 내용입니다.";
        DetectionInput input = new DetectionInput(inputText, DetectionType.CASE);

        List<Float> dummyEmbedding = List.of(0.1f, 0.2f, 0.3f);
        List<VectorDbClient.SearchResult> mockResults = List.of(
                new VectorDbClient.SearchResult("575aa23f-28bd-4673-ad9d-87e7921f8ee5", 0.47f, 
                    Map.of("CATEGORY", "피싱", "CONTENT", "의심스러운 피싱 사례", "data_version", "v2"))
        );

        given(vectorDbClient.embed(inputText)).willReturn(dummyEmbedding);
        given(vectorDbClient.searchSimilarCases(dummyEmbedding, 5)).willReturn(mockResults);
        given(detectionMapper.mapToStatus(mockResults)).willReturn(DetectionStatus.WARNING);
        given(detectionMapper.mapToCategory(mockResults)).willReturn(DetectionCategory.PHISHING);

        // when
        DetectionResult result = caseDetectionService.detect(input, kakaoId);

        // then
        assertThat(result.status()).isEqualTo(DetectionStatus.WARNING);
        assertThat(result.group()).isEqualTo(DetectionGroup.PHISHING);

        then(historyRepository).should().save(any(DetectionHistory.class));
    }

    @Test
    public void CASE_타입이_아닌_입력은_예외를_던진다() {
        // given
        DetectionInput input = new DetectionInput("http://example.com", DetectionType.URL);

        // when + then
        assertThatThrownBy(() -> caseDetectionService.detect(input, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_TYPE_CASE.getMessage());
    }
}
