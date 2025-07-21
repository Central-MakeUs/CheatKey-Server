package com.cheatkey.module.file.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "파일 업로드 요청")
public class FileUploadRequest {

    @NotBlank(message = "저장할 폴더 경로는 필수입니다.")
    @Schema(description = "저장할 폴더 경로 (예: community, profile)", required = true, example = "community")
    private String folder;

    @NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "사용자 ID", required = true, example = "1")
    private Long userId;

    @Schema(description = "업로드 폴더명 (community, profile, banner, temp 등)", example = "community")
    private String folderType;
} 