package com.cheatkey.module.detection.interfaces.controller;

import com.cheatkey.module.detection.domain.entity.DetectionInput;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.service.CaseDetectionService;
import com.cheatkey.module.detection.domain.service.UrlDetectionService;
import com.cheatkey.module.detection.interfaces.dto.CaseDetectionRequest;
import com.cheatkey.module.detection.interfaces.dto.DetectionResponse;
import com.cheatkey.module.detection.interfaces.dto.UrlDetectionRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/detection")
@Tag(name = "Detection", description = "사기 검색 관련 API")
public class DetectionController {

    private final UrlDetectionService urlDetectionService;
    private final CaseDetectionService caseDetectionService;

    @PostMapping("/url")
    public ResponseEntity<DetectionResponse> detectUrl(@Valid @RequestBody UrlDetectionRequest urlDetectionRequest) {
        DetectionInput input = new DetectionInput(urlDetectionRequest.getDetectionUrl(), DetectionType.URL);

        // @TODO 로그인 유저 검색 방식 수정
        Long userId = 99999L;

        DetectionResult result = urlDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }

    @PostMapping("/case")
    public ResponseEntity<DetectionResponse> detect(@RequestBody CaseDetectionRequest caseDetectionRequest) {
        DetectionInput input = new DetectionInput(caseDetectionRequest.getText(), DetectionType.CASE);

        // @TODO 로그인 유저 검색 방식 수정
        Long userId = 1L;

        DetectionResult result = caseDetectionService.detect(input, userId);
        return ResponseEntity.ok(new DetectionResponse(result));
    }
}
