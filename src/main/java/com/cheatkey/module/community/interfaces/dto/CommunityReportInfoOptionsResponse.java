package com.cheatkey.module.community.interfaces.dto;

import static com.cheatkey.common.code.interfaces.dto.OptionsResponse.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommunityReportInfoOptionsResponse {
    @Schema(description = "커뮤니티 신고하기 옵션 목록")
    List<Option> reportCodeList;
}
