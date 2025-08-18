package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.ValidationResult;
import com.cheatkey.module.detection.domain.entity.ValidationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIValidationService {
    
    private final DetectionOpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationResult validateInput(String userInput) {
        String prompt = String.format("""
            다음 입력이 피싱, 사기, 의심스러운 메시지, 링크, 계좌 정보 요구 등과 관련된 내용인지 판단하세요. 
            일반적인 인사말, 수학 문제, 게임 관련 등 명백히 관련 없는 경우만 거부하세요.

            입력: "%s"

            판단 기준:
            - 피싱/사기 관련 키워드 포함 → VALID_CASE
            - 의심스러운 링크/사이트 언급 → VALID_CASE  
            - 계좌, 비밀번호, 결제 등 금융 정보 요구 → VALID_CASE
            - 명백히 무관(인사말, 수학문제, 게임 대화 등) → INVALID_CASE
            - 불분명하거나 정보 부족 → NEEDS_CLARIFICATION

            다음 중 하나로 분류하세요:
            - VALID_CASE: 유효한 피싱 사례 분석 요청
            - INVALID_CASE: 피싱 사례와 무관한 요청
            - NEEDS_CLARIFICATION: 맥락이 불분명하여 추가 설명 필요

            응답 형식:
            {
                "classification": "VALID_CASE|INVALID_CASE|NEEDS_CLARIFICATION",
                "reason": "판단 근거",
                "confidence": 0.0-1.0,
                "suggestion": "사용자에게 제안할 내용"
            }
            """, userInput);
        
        try {
            String response = openAiService.generateResponse(prompt);
            return parseValidationResponse(response);
        } catch (Exception e) {
            log.warn("OpenAI 검증 실패, 기본 검증 결과 반환", e);
            return createDefaultValidationResult(userInput);
        }
    }
    
    private ValidationResult parseValidationResponse(String response) {
        try {
            // 1차: JSON 파싱 시도
            JsonNode jsonNode = objectMapper.readTree(response);
            
            String classification = jsonNode.get("classification").asText();
            String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "OpenAI 검증 성공";
            double confidence = jsonNode.has("confidence") ? jsonNode.get("confidence").asDouble() : 0.9;
            String suggestion = jsonNode.has("suggestion") ? jsonNode.get("suggestion").asText() : "";
            
            ValidationType validationType = parseValidationType(classification);
            boolean isValid = validationType == ValidationType.VALID_CASE;
            
            return ValidationResult.builder()
                .isValid(isValid)
                .reason(reason)
                .suggestion(suggestion)
                .validationType(validationType)
                .confidence(confidence)
                .build();
                
        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 키워드 기반 파싱으로 폴백", e);
            return parseByKeywords(response);
        }
    }
    
    private ValidationResult parseByKeywords(String response) {
        // 키워드 기반 파싱 (폴백 메커니즘)
        String upperResponse = response.toUpperCase();
        
        if (ValidationType.VALID_CASE.equals(upperResponse)) {
            return ValidationResult.builder()
                .isValid(true)
                .reason("OpenAI 검증 성공 (키워드 기반)")
                .validationType(ValidationType.VALID_CASE)
                .confidence(0.8)
                .build();
        } else if (upperResponse.contains("INVALID_CASE")) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("피싱 사례와 무관한 요청 (키워드 기반)")
                .validationType(ValidationType.INVALID_CASE)
                .confidence(0.7)
                .build();
        } else if (upperResponse.contains("NEEDS_CLARIFICATION")) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("맥락이 불분명함 (키워드 기반)")
                .validationType(ValidationType.NEEDS_CLARIFICATION)
                .confidence(0.6)
                .build();
        } else {
            // 키워드도 찾을 수 없는 경우
            return ValidationResult.builder()
                .isValid(false)
                .reason("응답 형식을 파악할 수 없음")
                .validationType(ValidationType.OPENAI_ERROR)
                .confidence(0.3)
                .build();
        }
    }
    
    private ValidationType parseValidationType(String classification) {
        return switch (classification.toUpperCase()) {
            case "VALID_CASE" -> ValidationType.VALID_CASE;
            case "INVALID_CASE" -> ValidationType.INVALID_CASE;
            case "NEEDS_CLARIFICATION" -> ValidationType.NEEDS_CLARIFICATION;
            default -> ValidationType.OPENAI_ERROR;
        };
    }
    
    private ValidationResult createDefaultValidationResult(String reason) {
        return ValidationResult.builder()
            .isValid(false)
            .reason(reason)
            .suggestion("일시적 오류입니다. 잠시 후 다시 시도해주세요")
            .validationType(ValidationType.OPENAI_ERROR)
            .confidence(0.0)
            .build();
    }
}
