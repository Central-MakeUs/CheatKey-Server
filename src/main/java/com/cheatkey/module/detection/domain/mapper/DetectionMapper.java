package com.cheatkey.module.detection.domain.mapper;

import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DetectionMapper {

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
}

