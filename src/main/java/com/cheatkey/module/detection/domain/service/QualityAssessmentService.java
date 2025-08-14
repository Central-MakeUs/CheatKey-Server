package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.config.QualityAssessmentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityAssessmentService {
    
    private final QualityAssessmentConfig config;

    /**
     * 입력 품질 점수 계산 (10점 만점)
     */
    public double calculateInputQualityScore(String input) {
        // 무의미한 입력 감지 (즉시 0점)
        if (isMeaninglessInput(input)) {
            return 0.0;
        }
        
        // 피싱 관련성 없는 입력 감지 (낮은 점수)
        if (!isPhishingRelated(input)) {
            return 2.5; // 0.5 * 5.0
        }
        
        double score = 0.0;
        
        // 길이 점수 (0-2점) - 설정 기반
        score += calculateLengthScore(input) * 5.0; // 0-0.4점 → 0-2점
        
        // 피싱 관련성 점수 (0-3점) - 핵심 가치
        score += calculatePhishingRelevanceScore(input) * 5.0; // 0-0.6점 → 0-3점
        
        // 구체성 점수 (0-2점) - 설정 기반
        score += calculateSpecificityScore(input) * 5.0; // 0-0.4점 → 0-2점
        
        // 플랫폼/서비스 점수 (0-2점) - 설정 기반
        score += calculatePlatformScore(input) * 5.0; // 0-0.4점 → 0-2점
        
        // 성적 유혹/유출 키워드 점수 (0-1점) - 최신 트렌드
        score += calculateTemptationScore(input) * 5.0; // 0-0.2점 → 0-1점
        
        return Math.min(score, 10.0); // 최대 10점으로 제한
    }
    
    /**
     * 길이 점수 계산 (0-0.4점)
     */
    private double calculateLengthScore(String input) {
        // 설정 기반 길이 점수
        int length = input.length();
        if (length >= config.getMinGoodLength()) return 0.5;
        if (length >= config.getMinAcceptableLength()) return 0.3;
        if (length >= config.getMinLength()) return 0.1;
        return 0.0;
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
    
    private double calculateSpecificityScore(String input) {
        double score = 0.0;
        
        // 구체적 상황 묘사
        if (input.contains("받았는데") || input.contains("보냈는데")) score += 0.2;
        if (input.contains("클릭했는데") || input.contains("입력했는데")) score += 0.2;
        
        // 최신 피싱 상황 패턴
        if (input.contains("가입했어요") || input.contains("등록했어요")) score += 0.2;
        if (input.contains("요구합니다") || input.contains("달라고")) score += 0.2;
        if (input.contains("소개받은") || input.contains("추천받은")) score += 0.1;
        
        return Math.min(score, 0.4);
    }
    
    private double calculatePlatformScore(String input) {
        double score = 0.0;
        
        // 기존 플랫폼/서비스
        if (input.contains("이메일") || input.contains("문자") || input.contains("전화")) score += 0.2;
        if (input.contains("은행") || input.contains("카드") || input.contains("결제")) score += 0.1;
        
        // 최신 플랫폼 확장
        if (input.contains("텔레그램") || input.contains("라인") || input.contains("카카오톡")) score += 0.2;
        if (input.contains("오픈채팅") || input.contains("사이트") || input.contains("앱")) score += 0.2;
        if (input.contains("인스타그램") || input.contains("페이스북") || input.contains("트위터")) score += 0.1;
        
        return Math.min(score, 0.4);
    }
    
    /**
     * 피싱 관련성 점수 계산 (핵심 가치)
     */
    private double calculatePhishingRelevanceScore(String input) {
        double score = 0.0;
        String lowerInput = input.toLowerCase();
        
        // 기본 피싱 키워드
        if (lowerInput.contains("피싱") || lowerInput.contains("사기") || lowerInput.contains("사칭")) score += 0.3;
        if (lowerInput.contains("의심") || lowerInput.contains("이상") || lowerInput.contains("수상")) score += 0.2;
        
        // 금전적 위험 요소
        if (lowerInput.contains("계좌") || lowerInput.contains("비밀번호") || lowerInput.contains("카드")) score += 0.2;
        if (lowerInput.contains("송금") || lowerInput.contains("이체") || lowerInput.contains("결제")) score += 0.2;
        if (lowerInput.contains("부업") || lowerInput.contains("돈") || lowerInput.contains("수익")) score += 0.2;
        
        // 개인정보 유출 위험
        if (lowerInput.contains("개인정보") || lowerInput.contains("신분증") || lowerInput.contains("주민번호")) score += 0.2;
        if (lowerInput.contains("연락처") || lowerInput.contains("주소") || lowerInput.contains("생년월일")) score += 0.1;
        
        return Math.min(score, 0.6);
    }
    
    /**
     * 성적 유혹/유출 키워드 점수 계산 (최신 트렌드)
     */
    private double calculateTemptationScore(String input) {
        double score = 0.0;
        String lowerInput = input.toLowerCase();
        
        // 성적 유혹 키워드
        if (lowerInput.contains("섹파") || lowerInput.contains("섹시") || lowerInput.contains("외로움")) score += 0.1;
        if (lowerInput.contains("연애") || lowerInput.contains("소개팅") || lowerInput.contains("만남")) score += 0.1;
        if (lowerInput.contains("친구") || lowerInput.contains("대화") || lowerInput.contains("상담")) score += 0.1;
        
        // 유출/추천 키워드
        if (lowerInput.contains("추천") || lowerInput.contains("소개") || lowerInput.contains("알려줘")) score += 0.1;
        if (lowerInput.contains("가입") || lowerInput.contains("등록") || lowerInput.contains("신청")) score += 0.1;
        
        return Math.min(score, 0.2);
    }
}
