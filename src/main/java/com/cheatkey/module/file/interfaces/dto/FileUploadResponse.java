package com.cheatkey.module.file.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "파일 업로드 응답")
public class FileUploadResponse {

    @Schema(description = "파일 업로드 ID", example = "1")
    private Long fileUploadId;

    @Schema(description = "원본 파일명", example = "image.jpg")
    private String originalName;

    @Schema(description = "S3 파일 키", example = "uploads/temp/community/62/2025/08/08/uuid-image.jpg")
    private String s3Key;

    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long size;

    @Schema(description = "파일 타입", example = "image/jpeg")
    private String contentType;

    @Schema(description = "임시 파일 여부", example = "true")
    private Boolean isTemp;

    @Schema(description = "업로드 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
} 