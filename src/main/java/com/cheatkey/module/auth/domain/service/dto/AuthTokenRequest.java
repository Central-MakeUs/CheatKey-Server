package com.cheatkey.module.auth.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenRequest {
    private String idToken;
    private String accessToken; // 카카오만 사용, 애플은 null
}
