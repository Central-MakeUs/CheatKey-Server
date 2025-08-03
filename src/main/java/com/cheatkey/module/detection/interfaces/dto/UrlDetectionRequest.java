package com.cheatkey.module.detection.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UrlDetectionRequest {

    @NotBlank(message = "URL을 입력하지 않았어요")
    @Size(max = 100, message = "URL은 100자 이하로 입력해 주세요.")
    @Pattern(
        regexp = "(?i)^(https?://)?(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[\\p{L}\\p{N}\\-\\.]+(\\.[\\p{L}\\p{N}\\-]+)+)(:\\d+)?(/[^\\s]*)?$",
        message = "URL을 입력하지 않았어요"
    )
    private String detectionUrl;

    public String getDetectionUrl() {
        return detectionUrl;
    }
}
