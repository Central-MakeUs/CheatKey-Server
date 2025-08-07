package com.cheatkey.module.mypage.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.common.exception.ErrorResponse;
import com.cheatkey.module.mypage.application.facade.MyPageFacade;
import com.cheatkey.module.mypage.interfaces.dto.*;
import com.cheatkey.module.terms.interfaces.dto.TermsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 대시보드 조회 성공", content = @Content(schema = @Schema(implementation = MyPageDashboardResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyPageDashboardResponse> getMyPageDashboard() {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        MyPageDashboardResponse response = myPageFacade.getMyPageDashboard(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/userInfo")
    @Operation(summary = "(★) 사용자 정보 수정", description = "닉네임과 프로필 이미지를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (닉네임 형식 오류, 중복 닉네임)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "프로필 이미지를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        myPageFacade.updateUserInfo(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/terms")
    @Operation(summary = "(★) 이용약관 조회", description = "모든 이용약관 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이용약관 조회 성공", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<TermsResponse>> getTerms() {
        List<TermsResponse> response = myPageFacade.getTerms();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/community/posts/management")
    @Operation(summary = "(★) 작성글 관리 조회", description = "본인이 작성한 글 목록과 통계 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성글 관리 조회 성공", content = @Content(schema = @Schema(implementation = UserPostManagementResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserPostManagementResponse> getUserPostManagement(Pageable pageable) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        UserPostManagementResponse response = myPageFacade.getUserPostManagement(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detection/history")
    @Operation(summary = "(★) 분석 내역 조회", description = "AI 분석 히스토리 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 내역 조회 성공", content = @Content(schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<DetectionHistoryResponse>> getDetectionHistory(
            @RequestParam(defaultValue = "today") @Parameter(description = "조회 기간", example = "today", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"today", "week", "month"})) String period,
            Pageable pageable) {
        Long userId = Long.parseLong(SecurityUtil.getCurrentUserId());
        Page<DetectionHistoryResponse> response = myPageFacade.getDetectionHistory(userId, period, pageable);
        return ResponseEntity.ok(response);
    }
}