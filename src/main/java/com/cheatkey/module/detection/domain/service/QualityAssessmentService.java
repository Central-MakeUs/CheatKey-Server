package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.QualityGrade;
import com.cheatkey.module.detection.domain.entity.QualityValidationMethod;
import com.cheatkey.module.detection.domain.config.QualityAssessmentConfig;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityAssessmentService {
    
    private final QualityAssessmentConfig config;
    
    public QualityAssessment assessQuality(List<VectorDbClient.SearchResult> results, String userInput) {
        QualityAssessment assessment = new QualityAssessment();
        
        if (results.isEmpty()) {
            assessment.setOverallScore(0.0);
            assessment.setQualityGrade(QualityGrade.UNACCEPTABLE);
            assessment.setReason("검색 결과가 없습니다");
            assessment.setSuggestion("유사한 피싱 사례를 찾을 수 없습니다. 새로운 사례로 등록하여 향후 참고할 수 있습니다.");
            assessment.setAcceptable(false);
            assessment.setAssessmentTime(LocalDateTime.now());
            assessment.setValidationMethod(QualityValidationMethod.BACKEND_ONLY);
            return assessment;
        }
        
        // 유사도 점수 기반 품질 계산
        float topScore = results.get(0).score();
        double qualityScore = calculateQualityScore(topScore, results.size(), userInput);
        
        // 유사도 점수가 너무 낮은 경우 특별 처리
        if (topScore < 0.3f) {
            qualityScore = Math.min(qualityScore, 3.0); // 최대 3점으로 제한
            assessment.setReason("검색 결과의 유사도가 매우 낮습니다");
            assessment.setSuggestion("더 구체적인 상황을 설명해주시거나 새로운 사례로 등록해주세요");
        }
        
        assessment.setOverallScore(qualityScore);
        assessment.setQualityGrade(QualityGrade.fromScore(qualityScore));
        assessment.setTopSimilarityScore(topScore);
        assessment.setResultCount(results.size());
        assessment.setAcceptable(qualityScore >= config.getMinAcceptableScore());
        assessment.setAssessmentTime(LocalDateTime.now());
        
        // 품질에 따른 상세 분석
        analyzeQualityDetails(assessment, results, userInput);
        
        return assessment;
    }
    
    private double calculateQualityScore(float topScore, int resultCount, String userInput) {
        // 유사도 점수 (0-6점) - 가중치 60%
        double similarityScore = topScore * 6.0;
        
        // 결과 개수 점수 (0-2점) - 가중치 20%
        double countScore = calculateCountScore(resultCount);
        
        // 입력 품질 점수 (0-2점) - 가중치 20%
        double inputQualityScore = calculateInputQualityScore(userInput);
        
        // 가중 평균 계산
        double weightedScore = (similarityScore * 0.6) + (countScore * 0.2) + (inputQualityScore * 0.2);
        
        return Math.min(weightedScore, 10.0);
    }
    
    private double calculateCountScore(int resultCount) {
        if (resultCount >= 5) return 2.0;
        if (resultCount >= 3) return 1.5;
        if (resultCount >= 1) return 1.0;
        return 0.0;
    }
    
    private double calculateInputQualityScore(String input) {
        // 무의미한 입력 감지 (즉시 0점)
        if (isMeaninglessInput(input)) {
            return 0.0;
        }
        
        // 피싱 관련성 없는 입력 감지 (낮은 점수)
        if (!isPhishingRelated(input)) {
            return 0.5;
        }
        
        double score = 0.0;
        
        // 길이 점수 (0-0.5점) - 설정 기반
        score += calculateLengthScore(input);
        
        // 질문 의도 점수 (0-0.5점) - 설정 기반
        score += calculateIntentScore(input);
        
        // 구체성 점수 (0-0.5점) - 설정 기반
        score += calculateSpecificityScore(input);
        
        // 플랫폼/서비스 점수 (0-0.5점) - 설정 기반
        score += calculatePlatformScore(input);
        
        return score;
    }
    
    private boolean isMeaninglessInput(String input) {
        // 무의미한 반복 패턴
        if (input.length() >= 3) {
            for (int i = 0; i < input.length() - 2; i++) {
                if (input.charAt(i) == input.charAt(i + 1) && 
                    input.charAt(i + 1) == input.charAt(i + 2)) {
                    return true;
                }
            }
        }
        
        // 일반적인 인사말
        String[] greetings = {"안녕하세요", "안녕", "반갑습니다", "하이", "hi", "hello"};
        String lowerInput = input.toLowerCase();
        for (String greeting : greetings) {
            if (lowerInput.contains(greeting.toLowerCase())) {
                return true;
            }
        }
        
        // 특수문자만 있는 경우
        if (input.replaceAll("[^!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]", "").length() > input.length() * 0.7) {
            return true;
        }
        
        return false;
    }
    
    private boolean isPhishingRelated(String input) {
        // 피싱 관련 키워드가 하나라도 있는지 확인
        String[] phishingKeywords = {
            "피싱", "사기", "사칭", "의심", "이상", "수상", "메시지", "링크", "클릭",
            "계좌", "비밀번호", "카드", "결제", "은행", "금액", "송금", "이체",
            "이메일", "문자", "전화", "알림", "경고", "주의", "확인", "검증"
        };
        
        String lowerInput = input.toLowerCase();
        for (String keyword : phishingKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    private double calculateLengthScore(String input) {
        // 설정 기반 길이 점수
        int length = input.length();
        if (length >= config.getMinGoodLength()) return 0.5;
        if (length >= config.getMinAcceptableLength()) return 0.3;
        if (length >= config.getMinLength()) return 0.1;
        return 0.0;
    }
    
    private double calculateIntentScore(String input) {
        double score = 0.0;
        
        // 질문 의도
        if (input.contains("?") || input.contains("무엇") || input.contains("어떻게")) score += 0.3;
        if (input.contains("왜") || input.contains("언제")) score += 0.2;
        
        return Math.min(score, 0.5);
    }
    
    private double calculateSpecificityScore(String input) {
        double score = 0.0;
        
        // 구체적 상황 묘사
        if (input.contains("받았는데") || input.contains("보냈는데")) score += 0.3;
        if (input.contains("클릭했는데") || input.contains("입력했는데")) score += 0.2;
        
        return Math.min(score, 0.5);
    }
    
    private double calculatePlatformScore(String input) {
        double score = 0.0;
        
        // 플랫폼/서비스
        if (input.contains("이메일") || input.contains("문자") || input.contains("전화")) score += 0.3;
        if (input.contains("은행") || input.contains("카드") || input.contains("결제")) score += 0.2;
        
        return Math.min(score, 0.5);
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
        
        // 개선 단계 제안
        assessment.setImprovementSteps(generateImprovementSteps(score, userInput));
    }
    
    private List<String> generateImprovementSteps(double score, String userInput) {
        List<String> steps = new ArrayList<>();
        
        if (score < 5.0) {
            steps.add("구체적인 시간과 장소를 명시해주세요");
            steps.add("어떤 플랫폼이나 서비스를 이용했는지 알려주세요");
            steps.add("의심스러웠던 구체적인 행동이나 메시지를 설명해주세요");
        }
        
        if (score < 7.0) {
            steps.add("금액이나 계좌 정보 등 구체적인 세부사항을 포함해주세요");
            steps.add("연락받은 방법(이메일, 문자, 전화 등)을 명시해주세요");
        }
        
        return steps;
    }
}
