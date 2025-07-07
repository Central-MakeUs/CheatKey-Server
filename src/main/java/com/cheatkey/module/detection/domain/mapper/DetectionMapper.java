package com.cheatkey.module.detection.domain.mapper;

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
}

