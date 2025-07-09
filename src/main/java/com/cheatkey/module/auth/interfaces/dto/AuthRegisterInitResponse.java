package com.cheatkey.module.auth.interfaces.dto;

import com.cheatkey.module.terms.interfaces.dto.TermsDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

import static com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 초기 정보 응답")
public class AuthRegisterInitResponse {

    @Schema(description = "연령대 옵션 목록")
    List<Option> ageCodeList;

    @Schema(description = "성별 옵션 목록")
    List<Option> genderCodeList;

    @Schema(description = "거래 방식 옵션 목록")
    List<Option> tradeMethodCodeList;

    @Schema(description = "거래 품목 옵션 목록")
    List<Option> tradeItemCodeList;

    @Schema(description = "이용 약관 목록")
    private List<TermsDto> termsList;
}