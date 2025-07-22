package com.cheatkey.module.community.interfaces.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityCommentRequest {
    @Schema(description = "댓글/대댓글이 달릴 게시글 ID", example = "1")
    @NotNull
    private Long postId;

    @Schema(description = "부모 댓글 ID (null이면 댓글, 값이 있으면 대댓글)", example = "10")
    private Long parentId;

    @Schema(description = "댓글/대댓글 내용 (최대 200자)", example = "이 글 정말 유용하네요!", maxLength = 200)
    @NotBlank
    @Size(max = 200)
    private String content;
}
