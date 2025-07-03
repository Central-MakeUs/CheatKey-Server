package com.cheatkey.module.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "회원가입 초기 정보 응답")
public class AuthRegisterInitResponse {

    @Schema(description = "카카오 ID", example = "1234567890")
    private Long kakaoId;

    @Schema(description = "카카오 닉네임", example = "cheatkey_user")
    private String kakaoName;
}