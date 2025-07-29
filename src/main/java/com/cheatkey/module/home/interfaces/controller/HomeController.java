package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.module.home.application.facade.HomeFacade;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "(★) Home", description = "홈 대시보드 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeFacade homeFacade;

    @Operation(summary = "(★) 메인 대시보드 조회", description = "사용자 정보, 인기글 목록을 포함한 메인 대시보드 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메인 대시보드 조회 성공", content = @Content(schema = @Schema(implementation = HomeDashboardResponse.class)))
    })
    @GetMapping("/dashboard")
    public ResponseEntity<HomeDashboardResponse> getDashboard() {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        HomeDashboardResponse response = homeFacade.getDashboard(userId);
        return ResponseEntity.ok(response);
    }
} 