package com.cheatkey.module.detection.interfaces.dto;

import com.cheatkey.module.detection.domain.entity.DetectionGroup;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
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

    public DetectionResponse(DetectionResult result) {
        this.detectionId = result.detectionId();
        this.status = result.status();
        this.group = result.group();
    }
}
