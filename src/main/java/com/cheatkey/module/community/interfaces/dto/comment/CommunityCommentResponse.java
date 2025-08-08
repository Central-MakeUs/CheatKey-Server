package com.cheatkey.module.community.interfaces.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommunityCommentResponse {
    @Schema(description = "댓글/대댓글 ID", example = "100")
    private Long id;

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "부모 댓글 ID (null이면 댓글)", example = "10")
    private Long parentId;

    @Schema(description = "작성자 authorId", example = "2")
    private Long authorId;

    @Schema(description = "작성자 닉네임", example = "테스트유저")
    private String authorNickname;

    @Schema(description = "댓글/대댓글 내용", example = "이 글 정말 유용하네요!")
    private String content;

    @Schema(description = "상태 (ACTIVE, DELETED)", example = "ACTIVE")
    private String status;

    @Schema(description = "작성일시", example = "2024-05-01T12:34:56")
    private LocalDateTime createdAt;

    @Schema(description = "대댓글 리스트 (자식 댓글)")
    private List<CommunityCommentResponse> children;
}
