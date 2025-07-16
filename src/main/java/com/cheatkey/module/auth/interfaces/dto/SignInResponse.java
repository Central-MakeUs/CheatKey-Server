package com.cheatkey.module.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponse {
    @Schema(description = "회원 상태 (ACTIVE: 정식회원, PENDING: 가입대기)", example = "ACTIVE")
    private String memberState;

    @Schema(description = "토큰 타입 (항상 Bearer)", example = "Bearer")
    private String grantType;

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
} 