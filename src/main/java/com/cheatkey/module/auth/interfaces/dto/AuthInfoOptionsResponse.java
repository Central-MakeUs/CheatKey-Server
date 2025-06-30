package com.cheatkey.module.auth.interfaces.dto;


import com.cheatkey.common.code.domain.entity.Code;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AuthInfoOptionsResponse {

    public record Option(String code, String name) {
        public static Option from(Code code) {
            return new Option(code.getCode(), code.getCodeName());
        }
    }

    public static List<Option> from(List<Code> codes) {
        return codes.stream()
                .map(Option::from)
                .toList();
    }
}
