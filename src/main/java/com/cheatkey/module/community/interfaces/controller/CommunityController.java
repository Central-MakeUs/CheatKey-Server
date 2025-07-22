package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.community.application.facade.CommunityPostFacade;
import com.cheatkey.module.community.domian.service.CommentService;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.CommunityPostBlockRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Community", description = "커뮤니티 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityPostFacade communityPostFacade;
    private final CommunityService communityService;

    @Operation(summary = "(★) 커뮤니티 글 작성", description = "커뮤니티 글을 작성합니다. 파일 첨부 가능.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "글 작성 성공, postId 반환", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/posts")
    public ResponseEntity<Long> createPost(@Valid @RequestBody CommunityPostCreateRequest request) throws ImageException {
        Long postId = communityPostFacade.createPostWithFiles(request);
        return ResponseEntity.ok(postId);
    }

    @Operation(summary = "게시글 신고하기", description = "게시글을 신고합니다. 신고 사유 코드는 t_code 테이블의 REPORT 그룹 코드 사용.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "신고 성공"),
        @ApiResponse(responseCode = "400", description = "이미 신고한 게시글/존재하지 않는 게시글 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<Void> reportPost(@PathVariable Long postId, @RequestBody CommunityPostReportRequest request) {
        // TODO: 인증 정보에서 reporterId 추출
        Long reporterId = request.getReporterId();
        communityService.reportPost(postId, reporterId, request.getReasonCode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글 작성자 차단(해당 유저 차단하기)", description = "특정 게시글 작성자를 차단합니다. 차단자는 해당 유저의 모든 게시글을 볼 수 없습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "차단 성공"),
        @ApiResponse(responseCode = "400", description = "이미 차단한 유저 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/users/{blockedId}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Long blockedId, @RequestBody CommunityPostBlockRequest request) {
        // TODO: 인증 정보에서 blockerId 추출
        Long blockerId = request.getBlockerId();
        communityService.blockUser(blockerId, blockedId, request.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글 삭제", description = "본인만 게시글을 삭제할 수 있습니다. 삭제 시 게시글은 노출되지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "존재하지 않는 게시글/본인 아님 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        // TODO: 인증 정보에서 userId 추출
        communityService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
