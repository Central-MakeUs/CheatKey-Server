package com.cheatkey.module.home.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "홈 화면 정보 응답")
public class HomeInitResponse {

    @Schema(description = "최초 회원가입 후 첫 로그인 여부", example = "true")
    private boolean welcome;

    @Schema(description = "회원 닉네임", example = "테스터")
    private String nickname;

    @Schema(description = "온보딩 완료 여부", example = "true")
    private boolean isOnboarded;

    @Schema(description = "사용자가 선택한 거래 방식 코드 목록", example = "[\"SNS\", \"APP\"]")
    private List<String> tradeMethodCodes;

    @Schema(description = "사용자가 선택한 거래 품목 코드 목록", example = "[\"FASHION\", \"LUXURY\"]")
    private List<String> tradeItemCodes;
}
