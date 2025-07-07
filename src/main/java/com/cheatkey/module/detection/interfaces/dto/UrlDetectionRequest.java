package com.cheatkey.module.detection.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UrlDetectionRequest {

    @NotBlank(message = "URL은 필수입니다.")
    @Size(max = 100, message = "URL은 100자 이하로 입력해 주세요.")
    private String url;
}
