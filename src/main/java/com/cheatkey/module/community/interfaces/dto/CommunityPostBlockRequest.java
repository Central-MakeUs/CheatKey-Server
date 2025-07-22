package com.cheatkey.module.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityPostBlockRequest {
    @Schema(description = "차단자 ID", example = "1")
    private Long blockerId;

    @Schema(description = "차단 사유", example = "HATE")
    private String reason;
} 