package com.cheatkey.module.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사용자 정보 수정 요청")
public class UpdateUserInfoRequest {
    
    @Schema(description = "닉네임", example = "테스터")
    @Size(min = 2, max = 5, message = "닉네임은 2~5자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임에 특수문자나 이모지는 사용할 수 없습니다.")
    private String nickname;
    
    @Schema(description = "프로필 이미지 ID", example = "1")
    private Long profileImageId;
} 