package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.community.domian.service.CommentService;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
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
import java.util.List;

@Tag(name = "(★) Community Comment", description = "커뮤니티 댓글/대댓글 관련 API")
@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityCommentController {
    private final CommentService commentService;
    private final AuthService authService;

    @Operation(summary = "(★) 댓글/대댓글 작성", description = "커뮤니티 게시글에 댓글 또는 대댓글을 작성합니다. parentId가 있으면 대댓글입니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "댓글 작성 성공, commentId 반환", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/comments")
    public ResponseEntity<Long> createComment(@Valid @RequestBody CommunityCommentRequest request) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        String userNickname = authService.getNickname(userId);
        Long commentId = commentService.createComment(request, userId, userNickname);
        return ResponseEntity.ok(commentId);
    }

    @Operation(summary = "(★) 댓글/대댓글 삭제", description = "댓글 또는 대댓글을 삭제합니다. 작성자 본인만 삭제할 수 있습니다. (소프트 딜리트)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "(★) 댓글/대댓글 조회", description = "게시글의 댓글 및 대댓글(트리 구조)을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "댓글/대댓글 조회 성공", content = @Content(schema = @Schema(implementation = CommunityCommentResponse.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommunityCommentResponse>> getCommentsForPost(@PathVariable Long postId) {
        List<CommunityCommentResponse> comments = commentService.getCommentsForPost(postId);
        return ResponseEntity.ok(comments);
    }
} 