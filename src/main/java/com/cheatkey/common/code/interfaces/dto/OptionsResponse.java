package com.cheatkey.common.code.interfaces.dto;

import com.cheatkey.common.code.domain.entity.Code;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OptionsResponse {
    @Schema(description = "선택지 응답 옵션")
    public record Option(String code, String name, String imageUrl, String disabledImageUrl) {
        public static Option from(Code code) {
            return new Option(
                code.getCode(), 
                code.getCodeName(),
                code.getImageUrl(),
                code.getDisabledImageUrl()
            );
        }
    }

    public static List<Option> from(List<Code> codes) {
        return codes.stream()
                .map(Option::from)
                .toList();
    }
}
