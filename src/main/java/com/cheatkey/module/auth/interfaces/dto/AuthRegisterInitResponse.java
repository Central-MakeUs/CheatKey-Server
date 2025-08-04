package com.cheatkey.module.auth.interfaces.dto;

import com.cheatkey.common.code.interfaces.dto.OptionsResponse.OptionInfo;
import com.cheatkey.module.terms.interfaces.dto.TermsResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "회원가입 초기 정보 응답")
public class AuthRegisterInitResponse {

    @Schema(description = "나이대 코드 목록")
    private List<OptionInfo> ageCodeList;

    @Schema(description = "성별 코드 목록")
    private List<OptionInfo> genderCodeList;

    @Schema(description = "거래 방식 코드 목록")
    private List<OptionInfo> tradeMethodCodeList;

    @Schema(description = "거래 품목 코드 목록")
    private List<OptionInfo> tradeItemCodeList;

    @Schema(description = "이용 약관 목록")
    private List<TermsResponse> termsList;
}