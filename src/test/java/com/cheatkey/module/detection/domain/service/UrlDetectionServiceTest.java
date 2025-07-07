package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.DetectionInput;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.infra.client.UrlDetectionClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.BDDMockito.given;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UrlDetectionServiceTest {

    @Mock
    private UrlDetectionClient urlDetectionClient;

    @InjectMocks
    private UrlDetectionService urlDetectionService;

    @Test
    void 위험한_URL_감지시_DANGER_반환() {
        // given
        String url = "http://malicious.com";
        DetectionInput input = new DetectionInput(url, DetectionType.URL);
        given(urlDetectionClient.checkUrl(url)).willReturn(true);

        // when
        DetectionResult result = urlDetectionService.detect(input);

        // then
        assertEquals(DetectionStatus.DANGER, result.status());
    }
}