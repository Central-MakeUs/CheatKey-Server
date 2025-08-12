package com.cheatkey.module.detection.domain.mapper;

import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.QualityGrade;
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
    
    // 새로운 품질 평가 메서드 추가
    public QualityAssessment assessQuality(List<VectorDbClient.SearchResult> results, String userInput) {
        QualityAssessment assessment = new QualityAssessment();
        
        if (results.isEmpty()) {
            assessment.setOverallScore(0.0);
            assessment.setQualityGrade(QualityGrade.UNACCEPTABLE);
            assessment.setReason("검색 결과가 없습니다");
            assessment.setSuggestion("다른 키워드나 표현으로 검색해보세요");
            assessment.setAcceptable(false);
            return assessment;
        }
        
        // 유사도 점수 기반 품질 계산
        float topScore = results.get(0).score();
        double qualityScore = calculateQualityScore(topScore, results.size(), userInput);
        
        assessment.setOverallScore(qualityScore);
        assessment.setQualityGrade(QualityGrade.fromScore(qualityScore));
        assessment.setTopSimilarityScore(topScore);
        assessment.setResultCount(results.size());
        assessment.setAcceptable(qualityScore >= 5.0); // 5점 이상을 수용 가능으로 판단
        
        // 품질에 따른 상세 분석
        analyzeQualityDetails(assessment, results, userInput);
        
        return assessment;
    }
    
    private double calculateQualityScore(float topScore, int resultCount, String userInput) {
        // 유사도 점수 (0-7점)
        double similarityScore = topScore * 7.0;
        
        // 결과 개수 점수 (0-2점)
        double countScore = Math.min(resultCount / 5.0, 2.0);
        
        // 입력 품질 점수 (0-1점)
        double inputQualityScore = assessInputQuality(userInput);
        
        return Math.min(similarityScore + countScore + inputQualityScore, 10.0);
    }
    
    private void analyzeQualityDetails(QualityAssessment assessment, List<VectorDbClient.SearchResult> results, String userInput) {
        double score = assessment.getOverallScore();
        
        if (score >= 8.0) {
            assessment.setReason("검색 결과가 매우 높은 관련성을 보임");
            assessment.setSuggestion("분석 결과를 신뢰할 수 있습니다");
        } else if (score >= 6.0) {
            assessment.setReason("검색 결과가 양호한 관련성을 보임");
            assessment.setSuggestion("결과를 참고하되 추가 검증을 권장합니다");
        } else if (score >= 4.0) {
            assessment.setReason("검색 결과가 제한적인 관련성을 보임");
            assessment.setSuggestion("더 구체적인 상황을 설명해주세요");
        } else {
            assessment.setReason("검색 결과의 관련성이 낮음");
            assessment.setSuggestion("다른 키워드나 표현으로 재검색해보세요");
        }
    }
    
    private double assessInputQuality(String input) {
        // 입력 품질 평가 (0-1점)
        double score = 0.0;
        
        if (input.length() >= 20) score += 0.3;
        if (input.contains("?") || input.contains("무엇") || input.contains("어떻게")) score += 0.3;
        if (input.contains("받았는데") || input.contains("보냈는데") || input.contains("클릭했는데")) score += 0.4;
        
        return score;
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

