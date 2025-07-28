package com.cheatkey.module.mypage.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.module.mypage.application.facade.MyPageFacade;
import com.cheatkey.module.mypage.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/v1/api/mypage")
@RequiredArgsConstructor
@Tag(name = "(★) My Page", description = "마이페이지 관련 API")
public class MyPageController {

    private final MyPageFacade myPageFacade;

    @GetMapping("/dashboard")
    @Operation(summary = "(★) 마이페이지 대시보드 조회", description = "사용자 정보와 프로필 이미지 목록을 조회합니다.")
    public ResponseEntity<MyPageDashboardResponse> getMyPageDashboard() {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        MyPageDashboardResponse response = myPageFacade.getMyPageDashboard(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/userInfo")
    @Operation(summary = "사용자 정보 수정", description = "닉네임과 프로필 이미지를 수정합니다.")
    public ResponseEntity<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        myPageFacade.updateUserInfo(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/community/posts/management")
    @Operation(summary = "(★) 작성글 관리 조회", description = "본인이 작성한 글 목록과 통계 정보를 조회합니다.")
    public ResponseEntity<UserPostManagementResponse> getUserPostManagement(Pageable pageable) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        UserPostManagementResponse response = myPageFacade.getUserPostManagement(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detection/history")
    @Operation(summary = "(★) 분석 내역 조회", description = "AI 분석 히스토리 목록을 조회합니다.")
    public ResponseEntity<Page<DetectionHistoryResponse>> getDetectionHistory(
            @RequestParam(defaultValue = "all") @Parameter(description = "조회 기간", example = "all", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"today", "week", "month", "all"})) String period,
            Pageable pageable) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        Page<DetectionHistoryResponse> response = myPageFacade.getDetectionHistory(userId, period, pageable);
        return ResponseEntity.ok(response);
    }
}