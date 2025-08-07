package com.cheatkey.module.detection.domain.mapper;

import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import com.cheatkey.module.mypage.interfaces.dto.DetectionDetailResponse;
import com.cheatkey.module.mypage.interfaces.dto.DetectionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DetectionMapper {

    private final VectorDbClient vectorDbClient;

    public DetectionStatus mapToStatus(List<VectorDbClient.SearchResult> results) {
        if (results.isEmpty()) return DetectionStatus.SAFE;

        float score = results.get(0).score();

        if (score >= 0.5f) return DetectionStatus.DANGER;
        if (score >= 0.3f) return DetectionStatus.WARNING;
        return DetectionStatus.SAFE;
    }

    public DetectionCategory mapToCategory(List<VectorDbClient.SearchResult> results) {
        if(results.isEmpty()) return DetectionCategory.PHISHING;

        String category = results.get(0).payload().get("CONTENT").toString();

        if(category.contains("거래")) {
            return DetectionCategory.TRANSACTION;
        } else if(category.contains("투자")) {
            return DetectionCategory.INVESTMENT;
        } else if(category.contains("사칭")) {
            return DetectionCategory.IMPERSONATION;
        } else {
            return DetectionCategory.PHISHING;
        }
    }

    public DetectionHistoryResponse toDetectionHistoryResponse(DetectionHistory history) {
        return DetectionHistoryResponse.builder()
                .id(history.getId())
                .status(history.getStatus())
                .detectionType(DetectionType.valueOf(history.getDetectionType()))
                .inputText(history.getInputText())
                .detectedAt(history.getDetectedAt())
                .topScore(history.getTopScore())
                .matchedCaseId(history.getMatchedCaseId())
                .group(history.getGroup())
                .build();
    }

    public List<DetectionHistoryResponse> toDetectionHistoryResponseList(List<DetectionHistory> histories) {
        return histories.stream()
                .map(this::toDetectionHistoryResponse)
                .collect(Collectors.toList());
    }

    public DetectionDetailResponse toDetectionDetailResponse(DetectionHistory history) {
        return DetectionDetailResponse.builder()
                .id(history.getId())
                .status(history.getStatus())
                .detectionType(DetectionType.valueOf(history.getDetectionType()))
                .inputText(history.getInputText())
                .topScore(history.getTopScore())
                .matchedCaseId(history.getMatchedCaseId())
                .detectedAt(history.getDetectedAt())
                .group(history.getGroup())
                .build();
    }
}

