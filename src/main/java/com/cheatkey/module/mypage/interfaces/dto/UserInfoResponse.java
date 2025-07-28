package com.cheatkey.module.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 정보 응답")
public class UserInfoResponse {
    
    @Schema(description = "닉네임", example = "테스터")
    private String nickname;
    
    @Schema(description = "총 방문 횟수", example = "42")
    private Integer totalVisitCount;

    @Schema(description = "사용자 레벨", example = "1")
    private Integer level;
    
    @Schema(description = "프로필 이미지 ID", example = "1")
    private Long profileImageId;
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;
    
    @Schema(description = "프로필 이미지 이름", example = "profile.jpg")
    private String profileImageName;
} 