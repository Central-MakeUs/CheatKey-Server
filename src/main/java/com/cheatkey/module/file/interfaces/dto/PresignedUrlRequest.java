package com.cheatkey.module.file.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Presigned URL 요청")
public class PresignedUrlRequest {

    @NotBlank(message = "S3 파일 키는 필수입니다.")
    @Schema(description = "S3 파일 키", required = true, example = "images/2024/01/15/uuid-image.jpg")
    private String fileKey;

    @NotNull(message = "만료 시간은 필수입니다.")
    @Min(value = 1, message = "만료 시간은 최소 1분 이상이어야 합니다.")
    @Schema(description = "만료 시간 (분), 기본값: 10분", example = "10")
    private Integer expirationInMinutes = 10;
} 