package com.cheatkey.module.detection.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사기 사례 유사도 검사 요청")
public class CaseDetectionRequest {

    @NotBlank(message = "사례 내용은 필수입니다.")
    @Size(max = 500, message = "텍스트는 500자 이하로 입력해 주세요.")
    @Schema(description = "검사할 텍스트 내용", example = "택배 지연 문자를 클릭하자 악성 앱이 설치되어 개인 정보가 유출됐어요. 조심하세요")
    private String text;
}
