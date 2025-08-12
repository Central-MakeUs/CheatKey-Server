package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.ValidationResult;
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
    
    public String improveQuery(String userInput, QualityAssessment quality) {
        String prompt = String.format("""
            다음 사용자 입력을 AI 피싱 사례 분석에 더 적합하도록 개선해주세요.
            
            원본 입력: "%s"
            
            현재 품질 점수: %.1f/10점
            품질 문제: %s
            
            개선 방향:
            1. 구체적인 상황 묘사 추가
            2. 시간, 장소, 플랫폼 등 세부사항 포함
            3. 의심스러웠던 구체적인 행동이나 메시지 설명
            4. 분석 가능한 구체적 내용 추가
            
            개선된 질문만 답변해주세요.
            """, userInput, quality.getOverallScore(), quality.getReason());
        
        try {
            return openAiService.generateResponse(prompt);
        } catch (Exception e) {
            log.warn("OpenAI를 통한 질문 개선 실패, 규칙 기반 검증으로 폴백", e);
            return validateAndImproveByRules(userInput);
        }
    }
    
    private String validateAndImproveByRules(String userInput) {
        // 무의미한 입력인 경우
        if (isMeaninglessInput(userInput)) {
            return "피싱 사례 분석과 관련 없는 입력입니다. 구체적인 의심스러운 상황을 설명해주세요.";
        }
        
        // 피싱 관련성이 낮은 경우
        if (!isPhishingRelated(userInput)) {
            return "피싱 사례 분석과 관련성이 낮습니다. 의심스러운 메시지나 링크, 계좌 관련 상황을 설명해주세요.";
        }
        
        // 기본적인 개선 제안
        return "더 구체적인 상황을 설명해주세요. 언제, 어디서, 어떤 상황이 의심스러웠는지 상세히 기술해주시면 정확한 분석이 가능합니다.";
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
    
    public ValidationResult validateInput(String userInput) {
        String prompt = String.format("""
            다음 사용자 입력이 AI 피싱 사례 분석을 요청하는 것인지 판단하세요.
            
            입력: "%s"
            
            판단 기준:
            1. 실제 피싱/사기 상황을 묘사하고 있는가?
            2. 구체적인 상황이나 증상이 포함되어 있는가?
            3. 분석이나 조언을 요청하는 의도가 있는가?
            
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
            
            ValidationResult.ValidationType validationType = parseValidationType(classification);
            boolean isValid = validationType == ValidationResult.ValidationType.VALID_CASE;
            
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
        
        if (upperResponse.contains("VALID_CASE")) {
            return ValidationResult.builder()
                .isValid(true)
                .reason("OpenAI 검증 성공 (키워드 기반)")
                .validationType(ValidationResult.ValidationType.VALID_CASE)
                .confidence(0.8)
                .build();
        } else if (upperResponse.contains("INVALID_CASE")) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("피싱 사례와 무관한 요청 (키워드 기반)")
                .validationType(ValidationResult.ValidationType.INVALID_CASE)
                .confidence(0.7)
                .build();
        } else if (upperResponse.contains("NEEDS_CLARIFICATION")) {
            return ValidationResult.builder()
                .isValid(false)
                .reason("맥락이 불분명함 (키워드 기반)")
                .validationType(ValidationResult.ValidationType.NEEDS_CLARIFICATION)
                .confidence(0.6)
                .build();
        } else {
            // 키워드도 찾을 수 없는 경우
            return ValidationResult.builder()
                .isValid(false)
                .reason("응답 형식을 파악할 수 없음")
                .validationType(ValidationResult.ValidationType.OPENAI_ERROR)
                .confidence(0.3)
                .build();
        }
    }
    
    private ValidationResult.ValidationType parseValidationType(String classification) {
        return switch (classification.toUpperCase()) {
            case "VALID_CASE" -> ValidationResult.ValidationType.VALID_CASE;
            case "INVALID_CASE" -> ValidationResult.ValidationType.INVALID_CASE;
            case "NEEDS_CLARIFICATION" -> ValidationResult.ValidationType.NEEDS_CLARIFICATION;
            default -> ValidationResult.ValidationType.OPENAI_ERROR;
        };
    }
    
    private ValidationResult createDefaultValidationResult(String reason) {
        return ValidationResult.builder()
            .isValid(false)
            .reason(reason)
            .suggestion("일시적 오류입니다. 잠시 후 다시 시도해주세요")
            .validationType(ValidationResult.ValidationType.OPENAI_ERROR)
            .confidence(0.0)
            .build();
    }
}
