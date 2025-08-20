package com.cheatkey.module.detection.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 분석 결과의 품질 평가 정보
 */
@Entity
@Data
@Table(name = "t_detection_quality_assessment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * AI 분석 결과의 전반적인 품질 점수 (0.0~10.0)
     *
     * 평가 기준:
     * - 검색 결과의 유사도 (40%)
     * - 사용자 입력의 명확성 (30%)
     * - AI 판정의 일관성 (20%)
     * - 시스템 신뢰도 (10%)
     *
     * 점수별 의미:
     * - 9.0~10.0: 우수한 품질, 즉시 적용 가능
     * - 7.0~8.9: 양호한 품질, 적용 가능
     * - 5.0~6.9: 수용 가능한 품질, 주의 필요
     * - 3.0~4.9: 낮은 품질, 추가 검증 필요
     * - 0.0~2.9: 매우 낮은 품질, 신뢰 불가
     */
    private double overallScore;

    // === 검색 관련 정보 ===
    private int searchAttempts;                 // 검색 시도 횟수
    private float preservedTopScore;            // 백터 검색 점수 (0.0~1.0)
    private int resultCount;                    // 검색 결과 수

    @Transient
    private List<String> improvementSteps;      // 개선 단계들

    // === 판정 결과 ===
    /**
     * 판정 결과의 신뢰성 여부
     * - true: 판정 결과를 신뢰하고 즉시 적용 가능
     * - false: 판정 결과를 신뢰할 수 없어 추가 검증 필요
     */
    private boolean isAcceptable;
    private LocalDateTime assessmentTime;       // 평가 시간
    /**
     * 행동 지침
     * 판정 결과에 따라 사용자가 취해야 할 구체적인 행동
     */
    private ActionType actionType;
}

