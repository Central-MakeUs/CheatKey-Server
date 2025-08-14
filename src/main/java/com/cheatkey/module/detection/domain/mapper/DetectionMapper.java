package com.cheatkey.module.detection.domain.mapper;

import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import com.cheatkey.module.mypage.interfaces.dto.DetectionDetailResponse;
import com.cheatkey.module.mypage.interfaces.dto.DetectionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DetectionMapper {

    public DetectionStatus mapToStatus(List<VectorDbClient.SearchResult> results) {
        if (results.isEmpty()) return DetectionStatus.SAFE;

        float score = results.get(0).score();

        if (score >= 0.75f) return DetectionStatus.DANGER;  // 7.5점 기준으로 수정
        if (score >= 0.5f) return DetectionStatus.WARNING;
        return DetectionStatus.SAFE;
    }

    public DetectionCategory mapToCategory(List<VectorDbClient.SearchResult> results) {
        if(results.isEmpty()) return DetectionCategory.PHISHING;

        // payload가 null이거나 CONTENT가 없는 경우 기본값 반환
        if(results.get(0).payload() == null || !results.get(0).payload().containsKey("CONTENT")) {
            return DetectionCategory.PHISHING;
        }

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

