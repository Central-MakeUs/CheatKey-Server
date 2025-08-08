package com.cheatkey.module.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 목록 응답")
public class CommunityPostListResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "사기 사례 공유합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "이런 사기를 당했습니다. 조심하세요...")
    private String content;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String authorNickname;

    @Schema(description = "등록일시", example = "2024-06-01T12:34:56")
    private LocalDateTime createdAt;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "대표 이미지 presignedUrl 리스트 (최대 5개)", example = "[\"https://cdn.../image1.jpg\", \"https://cdn.../image2.jpg\", \"https://cdn.../image3.jpg\", \"https://cdn.../image4.jpg\", \"https://cdn.../image5.jpg\"]")
    private List<String> thumbnailUrls;
}
