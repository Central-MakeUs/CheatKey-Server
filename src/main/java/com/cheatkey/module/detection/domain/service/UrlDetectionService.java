package com.cheatkey.module.detection.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.infra.client.UrlDetectionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlDetectionService {

    private final UrlDetectionClient urlDetectionClient;
    private final DetectionHistoryRepository detectionHistoryRepository;

    public DetectionResult detect(DetectionInput input, Long userId) {
        if (input.type() != DetectionType.URL || input.content() == null || input.content().isBlank() || !input.content().matches("(?i)^(https?://)?(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[\\p{L}\\p{N}\\-\\.]+(\\.[\\p{L}\\p{N}\\-]+)+)(:\\d+)?(/[^\\s]*)?$")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_TYPE_URL);
        }

        boolean isDanger = urlDetectionClient.checkUrl(input.content());
        DetectionStatus status = isDanger ? DetectionStatus.DANGER : DetectionStatus.SAFE;

        try {
            DetectionHistory history = DetectionHistory.builder()
                    .inputText(input.content())
                    .status(status)
                    .group(DetectionGroup.PHISHING)
                    .detectionType(DetectionType.URL.name())
                    .userId(userId)
                    .build();
            DetectionHistory detectionHistory = detectionHistoryRepository.save(history);

            return new DetectionResult(detectionHistory.getId(),status, DetectionGroup.PHISHING);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.DETECTION_FAILED);
        }
    }
}
