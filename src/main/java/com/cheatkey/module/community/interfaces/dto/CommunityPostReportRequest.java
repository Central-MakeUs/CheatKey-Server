package com.cheatkey.module.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "커뮤니티 게시글 신고 요청")
public class CommunityPostReportRequest {
    @Schema(description = "신고 사유 코드", example = "AD")
    private String reasonCode;
} 