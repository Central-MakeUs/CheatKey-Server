package com.cheatkey.module.file.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "파일 삭제 요청")
public class FileDeleteRequest {

    @NotBlank(message = "삭제할 파일의 S3 키는 필수입니다.")
    @Schema(description = "삭제할 파일의 S3 키", required = true, example = "images/2024/01/15/uuid-image.jpg")
    private String fileKey;
} 