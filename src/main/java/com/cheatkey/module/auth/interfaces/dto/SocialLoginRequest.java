package com.cheatkey.module.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialLoginRequest {
    @Schema(description = "소셜 로그인 제공자 (KAKAO, APPLE)", example = "KAKAO")
    @NotBlank
    private String provider;

    @Schema(description = "ID Token (OIDC)", example = "eyJhbGciOi...")
    @NotBlank
    private String idToken;

    @Schema(description = "Access Token (카카오만)", example = "eyJhbGciOi...")
    private String accessToken;
}
