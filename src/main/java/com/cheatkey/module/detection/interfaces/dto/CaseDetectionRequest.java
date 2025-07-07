package com.cheatkey.module.detection.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetectionRequest {

    @NotBlank(message = "사례 내용은 필수입니다.")
    @Size(max = 500, message = "텍스트는 500자 이하로 입력해 주세요.")
    private String text;
}
