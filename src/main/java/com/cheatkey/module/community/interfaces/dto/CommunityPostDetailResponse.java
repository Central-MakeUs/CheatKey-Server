package com.cheatkey.module.community.interfaces.dto;

import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import com.cheatkey.module.file.interfaces.dto.FileUploadResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 상세 응답")
public class CommunityPostDetailResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "사기 사례 공유합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "이런 사기를 당했습니다...")
    private String content;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String authorNickname;

    @Schema(description = "등록일시", example = "2024-06-01T12:34:56")
    private LocalDateTime createdAt;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "첨부파일 전체 정보 리스트")
    private List<FileUploadResponse> files;

    @Schema(description = "댓글/대댓글 리스트")
    private List<CommunityCommentResponse> comments;

    @Schema(description = "게시글 상태", example = "ACTIVE")
    private PostStatus status;

    @Schema(description = "삭제 가능 여부(내가 쓴 글인지)", example = "true")
    private boolean canDelete;

    @Schema(description = "차단 여부", example = "false")
    private boolean blocked;

    @Schema(description = "차단/신고 메시지", example = "차단된 글입니다.")
    private String blockMessage;
}
