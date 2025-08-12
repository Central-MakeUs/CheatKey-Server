package com.cheatkey.module.detection.interfaces.dto;

import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import com.cheatkey.module.detection.domain.entity.QualityGrade;
import com.cheatkey.module.detection.domain.entity.QualityValidationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "피싱 유사도 분석 결과")
public class DetectionResponse {

    private Long detectionId;

    @Schema(example = "SAFE")
    private final DetectionStatus status;

    @Schema(description = """
    사기 그룹
    - NORMAL: 거래&투자 사기
    - PHISHING: 피싱&사칭 사기""", example = "PHISHING")
    private final DetectionGroup group;

    // 새로운 품질 관련 필드 추가
    @Schema(description = "검색 품질 점수 (0-10점)", example = "7.5")
    private Double qualityScore;
    
    @Schema(description = "검색 품질 등급", example = "GOOD")
    private QualityGrade qualityGrade;
    
    @Schema(description = "품질 평가 이유", example = "검색 결과가 충분히 관련성이 높음")
    private String qualityReason;
    
    @Schema(description = "품질 개선 제안", example = "더 구체적인 상황을 설명해주세요")
    private String qualitySuggestion;
    
    @Schema(description = "검색 시도 횟수", example = "1")
    private Integer searchAttempts;
    
    @Schema(description = "최고 유사도 점수", example = "0.75")
    private Float topSimilarityScore;
    
    @Schema(description = "검색된 결과 개수", example = "3")
    private Integer resultCount;
    
    @Schema(description = "품질 검증 방법", example = "BACKEND_ONLY")
    private QualityValidationMethod validationMethod;
    
    // 커뮤니티 공유 관련 필드
    @Schema(description = "예상 위험도", example = "MEDIUM")
    private String estimatedRisk;
    
    @Schema(description = "커뮤니티 공유 권장 여부", example = "true")
    private Boolean shouldShareCommunity;
    
    @Schema(description = "추천 커뮤니티 카테고리", example = "[\"REPORT\", \"SHARE\"]")
    private String[] communityCategories;
    
    @Schema(description = "커뮤니티 공유 제목 제안", example = "새로운 피싱 사례 신고")
    private String communityShareTitle;
    
    @Schema(description = "커뮤니티 공유 안내 메시지", example = "이 상황을 커뮤니티에 공유하여 다른 사용자들에게 도움을 주세요")
    private String communityShareMessage;

    public DetectionResponse(DetectionResult result) {
        this.detectionId = result.detectionId();
        this.status = result.status();
        this.group = result.group();
        // 새로운 필드들은 기본값 null로 설정됨
    }
    
    // 새로운 생성자 (품질 정보 포함)
    public DetectionResponse(DetectionResult result, QualityAssessment qualityAssessment) {
        this.detectionId = result.detectionId();
        this.status = result.status();
        this.group = result.group();
        
        // 품질 정보 설정
        this.qualityScore = qualityAssessment.getOverallScore();
        this.qualityGrade = qualityAssessment.getQualityGrade();
        this.qualityReason = qualityAssessment.getReason();
        this.qualitySuggestion = qualityAssessment.getSuggestion();
        this.searchAttempts = qualityAssessment.getSearchAttempts();
        this.topSimilarityScore = qualityAssessment.getTopSimilarityScore();
        this.resultCount = qualityAssessment.getResultCount();
        this.validationMethod = qualityAssessment.getValidationMethod();
    }
}
