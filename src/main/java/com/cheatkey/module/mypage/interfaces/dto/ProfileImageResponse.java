package com.cheatkey.module.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "프로필 이미지 응답")
public class ProfileImageResponse {
    
    @Schema(description = "이미지 ID", example = "1")
    private Long id;
    
    @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
    private String imageUrl;
    
    @Schema(description = "이미지 이름", example = "profile.jpg")
    private String imageName;
} 