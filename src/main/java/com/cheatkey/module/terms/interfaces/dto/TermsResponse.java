package com.cheatkey.module.terms.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@Schema(description = "이용 약관 정보")
public class TermsResponse {

    @Schema(description = "이용 약관 ID")
    private Long id;

    @Schema(description = "이용 약관 제목")
    private String title;

    @Schema(description = "이용 약관 부제목")
    private String subTitle;

    @Schema(description = "이용 약관 내용")
    private String contents;

    @Schema(description = "이용 약관 필수/선택")
    private boolean required;

    @Schema(description = "이용 약관 버전 관리")
    private String version;
}
