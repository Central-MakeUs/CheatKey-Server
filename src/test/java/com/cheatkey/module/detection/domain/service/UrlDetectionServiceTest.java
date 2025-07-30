package com.cheatkey.module.detection.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.cheatkey.module.detection.domain.entity.DetectionHistory;

@ExtendWith(MockitoExtension.class)
class UrlDetectionServiceTest {

    @InjectMocks
    private UrlDetectionService urlDetectionService;

    @Mock
    private UrlDetectionClient urlDetectionClient;

    @Mock
    private DetectionHistoryRepository detectionHistoryRepository;

    @Test
    public void 위험한_URL_감지시_DANGER_반환() throws Exception {
        // given
        Long kakaoId = 99999L;
        String detectionUrl = "http://malicious.com";
        DetectionInput input = new DetectionInput(detectionUrl, DetectionType.URL);

        given(urlDetectionClient.checkUrl(detectionUrl)).willReturn(true);
        given(detectionHistoryRepository.save(any())).willAnswer(invocation -> {
            DetectionHistory h = invocation.getArgument(0);
            java.lang.reflect.Field idField = h.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(h, 1L);
            return h;
        });

        // when
        DetectionResult result = urlDetectionService.detect(input, kakaoId);

        // then
        assertEquals(DetectionStatus.DANGER, result.status());
        assertEquals(null, result.group());
        assertEquals(1L, result.detectionId());
        // 로그 이력 저장 여부 검증
        then(detectionHistoryRepository).should().save(any());
    }

    @Test
    void 빈값_URL_입력시_예외발생() {
        // given
        Long userId = 1L;
        DetectionInput input = new DetectionInput("", DetectionType.URL);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> urlDetectionService.detect(input, userId));
        assertEquals(ErrorCode.INVALID_INPUT_TYPE_URL, ex.getErrorCode());
        assertEquals("URL을 입력하지 않았어요", ex.getErrorCode().getMessage());
    }

    @Test
    void 잘못된_형식_URL_입력시_예외발생() {
        // given
        Long userId = 1L;
        DetectionInput input = new DetectionInput("not_a_url", DetectionType.URL);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> urlDetectionService.detect(input, userId));
        assertEquals(ErrorCode.INVALID_INPUT_TYPE_URL, ex.getErrorCode());
        assertEquals("URL을 입력하지 않았어요", ex.getErrorCode().getMessage());
    }

    @Test
    void null_URL_입력시_예외발생() {
        // given
        Long userId = 1L;
        DetectionInput input = new DetectionInput(null, DetectionType.URL);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> urlDetectionService.detect(input, userId));
        assertEquals(ErrorCode.INVALID_INPUT_TYPE_URL, ex.getErrorCode());
        assertEquals("URL을 입력하지 않았어요", ex.getErrorCode().getMessage());
    }
}