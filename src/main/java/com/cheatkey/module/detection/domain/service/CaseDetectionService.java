package com.cheatkey.module.detection.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.detection.domain.entity.*;
import com.cheatkey.module.detection.domain.mapper.DetectionMapper;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@RequiredArgsConstructor
@Service
public class CaseDetectionService {

    private final VectorDbClient vectorDbClient;
    private final DetectionMapper detectionMapper;
    private final DetectionHistoryRepository detectionHistoryRepository;

    @Transactional
    public DetectionResult detect(DetectionInput input, Long loginUserId) {

        if (input.type() != DetectionType.CASE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_TYPE_CASE);
        }

        try {
            List<Float> embedding = vectorDbClient.embed(input.content());
            List<VectorDbClient.SearchResult> results = vectorDbClient.searchSimilarCases(embedding, 5);

            float topScore = results.isEmpty() ? 0.0f : results.get(0).score();
            DetectionStatus status = detectionMapper.mapToStatus(results);

            DetectionCategory category = detectionMapper.mapToCategory(results);
            DetectionGroup group = DetectionGroup.NORMAL;
            if(category.isPhishingGroup()) {
                group = DetectionGroup.PHISHING;
            }

            DetectionHistory history = DetectionHistory.builder()
                    .inputText(input.content())
                    .topScore(topScore)
                    .status(status)
                    .detectionType(DetectionType.CASE.name())
                    .userId(loginUserId)
                    .matchedCaseId(results.isEmpty() ? null : results.get(0).id())
                    .build();
            detectionHistoryRepository.save(history);

            if (topScore >= 0.8f) {
                Map<String, Object> payload = Map.of(
                        "content", input.content(),
                        // @TODO 이후 카테고리 화 필요 시 추가
                        "source", "user-analyzed",
                        "userId", loginUserId
                );

                String uuid = UUID.randomUUID().toString();
                vectorDbClient.saveVector(uuid, embedding, payload);
            }

            return new DetectionResult(status, group);

        } catch (Exception e) {
            log.error("피싱 사례 분석 중 예외 발생", e);
            throw new CustomException(ErrorCode.DETECTION_FAILED);
        }
    }
}
