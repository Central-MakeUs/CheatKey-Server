package com.cheatkey.module.file.interfaces.controller;

import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.common.exception.ErrorResponse;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.service.FileService;
import com.cheatkey.module.file.interfaces.dto.FileDeleteRequest;
import com.cheatkey.module.file.interfaces.dto.FileUploadResponse;
import com.cheatkey.module.file.interfaces.dto.PresignedUrlRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "(★) File Upload", description = "파일 업로드 및 관리 API")
@RestController
@RequestMapping("/v1/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "(★) 파일 업로드", description = "파일을 S3에 업로드하고 Presigned URL을 반환합니다. 업로드된 파일은 임시 상태로 저장되며, 게시물 작성 완료 시 영구화됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(schema = @Schema(implementation = java.util.List.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 파일, 파일 크기 초과, 지원하지 않는 파일 형식)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 오류)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (파일 업로드 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> upload(@RequestParam("files") List<MultipartFile> files) throws ImageException {
        Long userId = Long.valueOf(SecurityUtil.getCurrentUserId());

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            FileUpload fileUpload = fileService.uploadTempFile(file, userId);
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileUploadId(fileUpload.getId())
                    .originalName(fileUpload.getOriginalName())
                    .size(fileUpload.getSize())
                    .contentType(fileUpload.getContentType())
                    .isTemp(fileUpload.getIsTemp())
                    .build();
            responses.add(response);
        }
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "(★) Presigned URL 재발급", description = "만료된 Presigned URL을 재발급합니다. 기본 만료 시간은 10분입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (Presigned URL 생성 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUrl(@Valid PresignedUrlRequest request) throws ImageException {
        URL presignedUrl = fileService.getPresignedUrl(request.getFileKey(), request.getExpirationInMinutes());
        return ResponseEntity.ok(presignedUrl.toString());
    }

    @Operation(summary = "(★) 영구 파일 Presigned URL 생성", description = "게시글에 포함된 영구 이미지의 Presigned URL을 생성합니다. 임시 파일은 접근할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (임시 파일 접근 시도)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (Presigned URL 생성 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/permanent-presigned-url")
    public ResponseEntity<String> getPermanentPresignedUrl(@Valid PresignedUrlRequest request) throws ImageException {
        URL presignedUrl = fileService.getPermanentFilePresignedUrl(request.getFileKey(), request.getExpirationInMinutes());
        return ResponseEntity.ok(presignedUrl.toString());
    }

    @Operation(summary = "(★) 파일 삭제", description = "S3에서 파일을 삭제합니다. 임시 파일이나 더 이상 필요하지 않은 파일을 삭제할 때 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (파일 삭제 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFile(@Valid @RequestBody FileDeleteRequest request) throws ImageException {
        fileService.deleteFile(request.getFileKey());
        return ResponseEntity.ok().build();
    }
}