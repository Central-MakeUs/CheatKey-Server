package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.code.domain.service.CodeService;
import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.community.application.facade.CommunityPostFacade;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.cheatkey.common.code.interfaces.dto.OptionsResponse.Option;

@Tag(name = "(★) Community", description = "커뮤니티 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityPostFacade communityPostFacade;
    private final CommunityService communityService;
    private final CodeService codeService;

    @Operation(summary = "(★) 커뮤니티 게시글 목록 조회", description = "정상(ACTIVE) 상태의 게시글 중 차단되지 않은 게시글만 페이징/검색/정렬 조건에 따라 조회합니다. 대표 이미지는 최대 5개 presignedUrl만 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공", content = @Content(schema = @Schema(implementation = CommunityPostListResponse.class)))
    })
    @GetMapping("/posts")
    public ResponseEntity<Page<CommunityPostListResponse>> getPosts(@ModelAttribute CommunityPostListRequest request) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        int page = Math.max(request.getPage() - 1, 0); // 1부터 시작
        Pageable pageable = PageRequest.of(page, request.getSize());

        Page<CommunityPostListResponse> posts = communityService.getPostList(userId, request.getKeyword(), request.getSort(), pageable);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "(★) 커뮤니티 게시글 상세 조회", description = "정상(ACTIVE) 상태의 게시글 상세 정보를 조회합니다. 첨부파일 전체, 댓글/대댓글, 차단/신고 정책, 삭제 가능 여부 등을 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공", content = @Content(schema = @Schema(implementation = CommunityPostDetailResponse.class)))
    })
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityPostDetailResponse> getPostDetail(@PathVariable Long postId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        CommunityPostDetailResponse detail = communityService.getPostDetail(userId, postId);
        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "(★) 게시글 신고하기 목록", description = "게시글 신고하기 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 신고하기 목록 조회 성공", content = @Content(schema = @Schema(implementation = CommunityReportInfoOptionsResponse.class)))
    })
    @GetMapping("/posts/report-reasons")
    public ResponseEntity<CommunityReportInfoOptionsResponse> getReportPost() {
        List<Option> repostCodeList = codeService.getOptionsByType(CodeType.REPORT);
        CommunityReportInfoOptionsResponse response = CommunityReportInfoOptionsResponse.builder()
                .reportCodeList(repostCodeList)
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "(★) 커뮤니티 글 작성", description = "커뮤니티 글을 작성합니다. 파일 첨부 가능.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "글 작성 성공, postId 반환", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/posts")
    public ResponseEntity<Long> createPost(@Valid @RequestBody CommunityPostCreateRequest request) throws ImageException {
        // @TODO 작성된 글 Vector DB 에 저장 (REPORT, SHARE 카테고리만 포함)
        Long postId = communityPostFacade.createPostWithFiles(request);
        return ResponseEntity.ok(postId);
    }

    @Operation(summary = "(★) 게시글 신고하기", description = "게시글을 신고합니다. 신고 사유 코드는 t_code 테이블의 REPORT 그룹 코드 사용.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "신고 성공"),
        @ApiResponse(responseCode = "400", description = "이미 신고한 게시글/존재하지 않는 게시글 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<Void> reportPost(@PathVariable Long postId, @RequestBody CommunityPostReportRequest request) {
        Long reporterId = Long.valueOf(SecurityUtil.getCurrentUserId());
        communityService.reportPost(postId, reporterId, request.getReasonCode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "(★) 게시글 작성자 차단(해당 유저 차단하기)", description = "특정 게시글 작성자를 차단합니다. 차단자는 해당 유저의 모든 게시글을 볼 수 없습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "차단 성공"),
        @ApiResponse(responseCode = "400", description = "이미 차단한 유저 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/users/{blockedId}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Long blockedId, @RequestBody CommunityPostBlockRequest request) {
        Long blockerId = Long.valueOf(SecurityUtil.getCurrentUserId());
        communityService.blockUser(blockerId, blockedId, request.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "(★) 게시글 삭제", description = "본인만 게시글을 삭제할 수 있습니다. 삭제 시 게시글은 노출되지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "존재하지 않는 게시글/본인 아님 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        communityService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}