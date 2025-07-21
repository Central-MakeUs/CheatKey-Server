package com.cheatkey.module.file.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "파일 업로드 응답")
public class FileUploadResponse {

    @Schema(description = "파일 ID", example = "1")
    private Long id;

    @Schema(description = "원본 파일명", example = "image.jpg")
    private String originalName;

    @Schema(description = "S3 파일 키", example = "images/2024/01/15/uuid-image.jpg")
    private String s3Key;

    @Schema(description = "Presigned URL", example = "https://bucket.s3.amazonaws.com/images/2024/01/15/uuid-image.jpg?X-Amz-Algorithm=...")
    private String presignedUrl;

    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long size;

    @Schema(description = "파일 타입", example = "image/jpeg")
    private String contentType;

    @Schema(description = "임시 파일 여부", example = "true")
    private boolean isTemp;

    @Schema(description = "업로드 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
} 