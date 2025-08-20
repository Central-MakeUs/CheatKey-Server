package com.cheatkey.module.detection.domain.service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OpenAICostTracker {
    
    // 메모리 기반 비용 추적 (Redis 대신)
    private final Map<String, Integer> dailyCallCounts = new ConcurrentHashMap<>();
    private final Map<String, Double> dailyCosts = new ConcurrentHashMap<>();
    
    // GPT-5 가격 정보 (2024년 기준)
    private static final double INPUT_TOKEN_COST_PER_MILLION = 0.05;    // $0.05 / 1M tokens
    private static final double OUTPUT_TOKEN_COST_PER_MILLION = 0.40;   // $0.40 / 1M tokens
    
    // 설정 가능한 비용 제한 (기본값)
    private double dailyCostLimit = 0.01;           // 일일 비용 제한 (달러)
    private int dailyCallLimit = 100;               // 일일 호출 제한
    private double singleCallCostLimit = 0.001;     // 단일 호출 비용 제한 (달러)
    
    public int getDailyCallCount() {
        String key = "openai:daily:calls:" + LocalDate.now();
        return dailyCallCounts.getOrDefault(key, 0);
    }
    
    public double getDailyCost() {
        String key = "openai:daily:cost:" + LocalDate.now();
        return dailyCosts.getOrDefault(key, 0.0);
    }
    
    public void incrementCallCount() {
        String key = "openai:daily:calls:" + LocalDate.now();
        dailyCallCounts.merge(key, 1, Integer::sum);
        
        // 오래된 데이터 정리 (7일 전 데이터 삭제)
        cleanupOldData();
    }
    
    public void addCost(double cost) {
        String key = "openai:daily:cost:" + LocalDate.now();
        dailyCosts.merge(key, cost, Double::sum);
        
        // 오래된 데이터 정리 (7일 전 데이터 정리)
        cleanupOldData();
    }
    
    /**
     * GPT-5 토큰 기반 비용 계산
     * @param inputTokens 입력 토큰 수
     * @param outputTokens 출력 토큰 수
     * @return 예상 비용 (달러)
     */
    public double calculateCost(int inputTokens, int outputTokens) {
        double inputCost = (inputTokens / 1_000_000.0) * INPUT_TOKEN_COST_PER_MILLION;
        double outputCost = (outputTokens / 1_000_000.0) * OUTPUT_TOKEN_COST_PER_MILLION;
        return inputCost + outputCost;
    }
    
    /**
     * 실제 응답 기반 비용 계산 (더 정확한 비용 추정)
     * @param prompt 프롬프트 텍스트
     * @param response 응답 텍스트
     * @return 예상 비용 (달러)
     */
    public double calculateCostFromResponse(String prompt, String response) {
        // 간단한 토큰 수 추정 (영어 기준: 1토큰 ≈ 4자, 한국어 기준: 1토큰 ≈ 2자)
        int inputTokens = estimateTokens(prompt);
        int outputTokens = estimateTokens(response);
        
        return calculateCost(inputTokens, outputTokens);
    }
    
    /**
     * 토큰 수 추정 (간단한 구현)
     * 실제 OpenAI API의 tiktoken과는 다를 수 있음
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        
        int koreanChars = 0;
        int englishChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL) {
                koreanChars++;
            } else if (Character.isLetter(c) && c <= 127) {
                englishChars++;
            } else {
                otherChars++;
            }
        }
        
        // 한국어: 1토큰 ≈ 2자, 영어: 1토큰 ≈ 4자, 기타: 1토큰 ≈ 3자
        return (koreanChars / 2) + (englishChars / 4) + (otherChars / 3) + 1;
    }
    
    /**
     * 질문 개선 API 호출 비용 계산 (실제 응답 기반)
     */
    public double getQueryImprovementCost() {
        // 기본 비용 (설정 가능)
        return Math.min(calculateCost(210, 80), singleCallCostLimit);
    }
    
    /**
     * 입력 검증 API 호출 비용 계산 (실제 응답 기반)
     */
    public double getInputValidationCost() {
        // 기본 비용 (설정 가능)
        return Math.min(calculateCost(250, 120), singleCallCostLimit);
    }
    
    public boolean canMakeCall(double estimatedCost) {
        try {
            double dailyCost = getDailyCost();
            int dailyCalls = getDailyCallCount();
            
            // 설정 기반 제한 확인
            boolean costLimitOk = dailyCost + estimatedCost <= dailyCostLimit;
            boolean callLimitOk = dailyCalls < dailyCallLimit;
            boolean singleCallLimitOk = estimatedCost <= singleCallCostLimit;
            
            if (!costLimitOk) {
                log.info("일일 비용 제한 초과: 현재 ${}, 제한 ${}", 
                        String.format("%.6f", dailyCost), String.format("%.6f", dailyCostLimit));
            }
            if (!callLimitOk) {
                log.info("일일 호출 제한 초과: 현재 {}, 제한 {}", dailyCalls, dailyCallLimit);
            }
            if (!singleCallLimitOk) {
                log.info("단일 호출 비용 제한 초과: 예상 ${}, 제한 ${}", 
                        String.format("%.6f", estimatedCost), String.format("%.6f", singleCallCostLimit));
            }
            
            return costLimitOk && callLimitOk && singleCallLimitOk;
            
        } catch (Exception e) {
            log.warn("비용 추적 중 오류 발생, OpenAI 호출 허용", e);
            return true; // 오류 발생 시 기본적으로 허용
        }
    }
    
    // 설정 메서드들 (런타임에 비용 제한 조정 가능)
    public void setDailyCostLimit(double limit) {
        this.dailyCostLimit = limit;
        log.info("일일 비용 제한이 ${}로 설정되었습니다", String.format("%.6f", limit));
    }
    
    public void setDailyCallLimit(int limit) {
        this.dailyCallLimit = limit;
        log.info("일일 호출 제한이 {}회로 설정되었습니다", limit);
    }
    
    public void setSingleCallCostLimit(double limit) {
        this.singleCallCostLimit = limit;
        log.info("단일 호출 비용 제한이 ${}로 설정되었습니다", String.format("%.6f", limit));
    }
    
    private void cleanupOldData() {
        LocalDate today = LocalDate.now();
        dailyCallCounts.entrySet().removeIf(entry -> {
            try {
                String dateStr = entry.getKey().split(":")[2];
                LocalDate entryDate = LocalDate.parse(dateStr);
                return entryDate.isBefore(today.minusDays(7));
            } catch (Exception e) {
                return true; // 파싱 실패 시 삭제
            }
        });
        
        dailyCosts.entrySet().removeIf(entry -> {
            try {
                String dateStr = entry.getKey().split(":")[2];
                LocalDate entryDate = LocalDate.parse(dateStr);
                return entryDate.isBefore(today.minusDays(7));
            } catch (Exception e) {
                return true; // 파싱 실패 시 삭제
            }
        });
    }
}
