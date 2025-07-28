package com.cheatkey.module.detection.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.module.detection.domain.entity.DetectionInput;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.service.CaseDetectionService;
import com.cheatkey.module.detection.domain.service.DetectionService;
import com.cheatkey.module.detection.domain.service.UrlDetectionService;
import com.cheatkey.module.detection.interfaces.dto.CaseDetectionRequest;
import com.cheatkey.module.detection.interfaces.dto.DetectionResponse;
import com.cheatkey.module.detection.interfaces.dto.UrlDetectionRequest;
import com.cheatkey.module.mypage.interfaces.dto.DetectionDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/detection")
@Tag(name = "Detection", description = "사기 검색 관련 API")
public class DetectionController {

    private final UrlDetectionService urlDetectionService;
    private final CaseDetectionService caseDetectionService;
    private final DetectionService detectionService;

    @Operation(summary = "URL 피싱 검사", description = "입력된 URL이 피싱 사이트인지 검사합니다.")
    @PostMapping("/url")
    public ResponseEntity<DetectionResponse> detectUrl(@Valid @RequestBody UrlDetectionRequest urlDetectionRequest) {
        DetectionInput input = new DetectionInput(urlDetectionRequest.getDetectionUrl(), DetectionType.URL);
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        DetectionResult result = urlDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }

    @Operation(summary = "사기 사례 유사도 검사", description = "입력된 텍스트와 유사한 사기 사례를 검색합니다.")
    @PostMapping("/case")
    public ResponseEntity<DetectionResponse> detect(@RequestBody CaseDetectionRequest caseDetectionRequest) {
        DetectionInput input = new DetectionInput(caseDetectionRequest.getText(), DetectionType.CASE);
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        DetectionResult result = caseDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }

    @Operation(summary = "분석 결과 상세 조회", description = "AI 분석 결과 상세 정보를 조회합니다.")
    @GetMapping("/history/{detectionId}")
    public ResponseEntity<DetectionDetailResponse> getDetectionDetail(@PathVariable Long detectionId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        DetectionDetailResponse response = detectionService.getDetectionDetail(userId, detectionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "분석 내역 상세 조회", description = "특정 AI 분석 결과의 상세 정보를 조회합니다.")
    @GetMapping("/history/{detectionId}/detail")
    public ResponseEntity<DetectionDetailResponse> getDetectionHistoryDetail(@PathVariable Long detectionId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        DetectionDetailResponse response = detectionService.getDetectionDetail(userId, detectionId);
        return ResponseEntity.ok(response);
    }
}
