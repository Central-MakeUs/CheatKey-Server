package com.cheatkey.module.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "사용자 작성글 응답")
public class UserPostResponse {
    
    @Schema(description = "게시글 ID", example = "1")
    private Long id;
    
    @Schema(description = "작성자 닉네임", example = "테스터")
    private String nickname;
    
    @Schema(description = "게시글 제목", example = "안녕하세요")
    private String title;
    
    @Schema(description = "게시글 내용", example = "첫 번째 게시글입니다.")
    private String content;
    
    @Schema(description = "작성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "댓글 수", example = "5")
    private Long commentCount;
    
    @Schema(description = "첨부된 이미지 URL 목록")
    private List<String> imageUrls;
} 