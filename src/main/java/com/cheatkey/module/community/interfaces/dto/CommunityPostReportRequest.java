package com.cheatkey.module.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "커뮤니티 게시글 신고 요청")
public class CommunityPostReportRequest {
    @Schema(description = "신고 사유 코드 (FAKE: 허위정보, HATE: 혐오표현, PRIVACY: 개인정보노출, SPAM: 도배성게시글, AD: 상업적광고, POLICY: 운영방침위반)", example = "AD", allowableValues = {
        "FAKE", "HATE", "PRIVACY", "SPAM", "AD", "POLICY"
    })
    private String reasonCode;
} 