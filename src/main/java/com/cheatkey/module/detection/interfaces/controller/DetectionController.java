package com.cheatkey.module.detection.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.common.exception.ErrorResponse;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/detection")
@Tag(name = "(★) Detection", description = "사기 검색 관련 API")
public class DetectionController {

    private final UrlDetectionService urlDetectionService;
    private final CaseDetectionService caseDetectionService;
    private final DetectionService detectionService;

    @Operation(summary = "(★) URL 피싱 검사", description = "입력된 URL이 피싱 사이트인지 검사합니다. Google Safe Browsing API와 내부 데이터베이스를 활용하여 분석합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL 검사 성공", content = @Content(schema = @Schema(implementation = DetectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (URL 형식 오류, 빈 URL)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Google Safe Browsing API 서비스 불가", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/url")
    public ResponseEntity<DetectionResponse> detectUrl(@Valid @RequestBody UrlDetectionRequest urlDetectionRequest) {
        DetectionInput input = new DetectionInput(urlDetectionRequest.getDetectionUrl(), DetectionType.URL);
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        DetectionResult result = urlDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }

    @Operation(summary = "(★) 사기 사례 유사도 검사", description = "입력된 텍스트와 유사한 사기 사례를 검색합니다. AI 임베딩을 활용하여 유사도를 분석합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사기 사례 검사 성공", content = @Content(schema = @Schema(implementation = DetectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 텍스트, 검사 타입 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/case")
    public ResponseEntity<DetectionResponse> detect(@RequestBody CaseDetectionRequest caseDetectionRequest) {
        DetectionInput input = new DetectionInput(caseDetectionRequest.getText(), DetectionType.CASE);
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        DetectionResult result = caseDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }

    @Operation(summary = "(★) 분석 결과 상세 조회", description = "AI 분석 결과의 상세 정보를 조회합니다. 본인의 분석 내역만 조회 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 결과 상세 조회 성공", content = @Content(schema = @Schema(implementation = DetectionDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 내역을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "조회 권한이 없음 (본인의 분석 내역이 아님)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history/{detectionId}")
    public ResponseEntity<DetectionDetailResponse> getDetectionDetail(@PathVariable Long detectionId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        DetectionDetailResponse response = detectionService.getDetectionDetail(userId, detectionId);
        return ResponseEntity.ok(response);
    }
}
