package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.DetectionInput;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.infra.client.UrlDetectionClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UrlDetectionServiceTest {

    @InjectMocks
    private UrlDetectionService urlDetectionService;

    @Mock
    private UrlDetectionClient urlDetectionClient;

    @Mock
    private DetectionHistoryRepository detectionHistoryRepository;

    @Test
    public void 위험한_URL_감지시_DANGER_반환() {
        // given
        Long kakaoId = 99999L;
        String url = "http://malicious.com";
        DetectionInput input = new DetectionInput(url, DetectionType.URL);

        given(urlDetectionClient.checkUrl(url)).willReturn(true);

        // when
        DetectionResult result = urlDetectionService.detect(input, kakaoId);

        // then
        assertEquals(DetectionStatus.DANGER, result.status());
        assertEquals("Google Safe Browsing API 응답 기반", result.reason());

        // 로그 이력 저장 여부 검증
        then(detectionHistoryRepository).should().save(any());
    }
}