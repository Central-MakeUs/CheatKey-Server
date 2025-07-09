package com.cheatkey.module.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuthRegisterRequest {

    @NotBlank
    @Size(min = 2, max = 5)
    @Schema(description = "닉네임", example = "테스트123")
    private String nickname;

    @NotBlank
    @Schema(description = "나이대 코드", example = "50_60")
    private String ageCode;

    @NotBlank
    @Schema(description = "성별 코드", example = "FEMALE")
    private String genderCode;

    @Schema(description = "거래 방식 코드 목록")
    private List<String> tradeMethodCodeList;

    @Schema(description = "거래 품목 코드 목록")
    private List<String> tradeItemCodeList;

    @Schema(description = "필수 항목 동의")
    private List<Long> agreedRequiredTerms;

    @Schema(description = "선택 항목 동의")
    private List<Long> agreedOptionalTerms;
}
