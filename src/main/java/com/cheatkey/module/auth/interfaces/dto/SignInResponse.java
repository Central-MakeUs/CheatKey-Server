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
@Schema(description = "소셜 로그인 응답")
public class SignInResponse {
    
    @Schema(description = "사용자 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "PENDING", "SUSPENDED"})
    private String userState;
    
    @Schema(description = "인증 타입", example = "Bearer")
    private String grantType;
    
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
} 