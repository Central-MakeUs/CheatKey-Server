package com.cheatkey.module.file.domain.service;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.entity.FileFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private S3FileService s3FileService;

    @Mock
    private FileUploadRepository fileUploadRepository;

    @InjectMocks
    private FileService fileService;

    private MockMultipartFile mockFile;
    private FileUpload mockFileUpload;
    private FileUpload permanentFile;
    private FileUpload tempFile;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        mockFileUpload = FileUpload.builder()
                .id(1L)
                .userId(1L)
                .originalName("test-image.jpg")
                .s3Key("test-images/2024/01/15/uuid-test-image.jpg")
                .presignedUrl("https://bucket.s3.amazonaws.com/test-images/2024/01/15/uuid-test-image.jpg")
                .folder("test-images")
                .size(1024L)
                .contentType("image/jpeg")
                .isTemp(true)
                .createdAt(LocalDateTime.now())
                .build();

        permanentFile = FileUpload.builder()
                .id(1L)
                .userId(1L)
                .originalName("permanent-image.jpg")
                .s3Key("test-images/2024/01/15/uuid-permanent-image.jpg")
                .isTemp(false)
                .build();

        tempFile = FileUpload.builder()
                .id(2L)
                .userId(1L)
                .originalName("temp-image.jpg")
                .s3Key("test-images/2024/01/15/uuid-temp-image.jpg")
                .isTemp(true)
                .build();
    }

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_Success() throws Exception {
        // given
        String expectedFileKey = "test-images/2024/01/15/uuid-test-image.jpg";
        String expectedPresignedUrl = "https://bucket.s3.amazonaws.com/test-images/2024/01/15/uuid-test-image.jpg";
        
        when(s3FileService.uploadFile(any(MockMultipartFile.class), any(FileFolder.class), anyBoolean()))
                .thenReturn(expectedFileKey);
        when(s3FileService.getPresignedUrl(anyString()))
                .thenReturn(new URL(expectedPresignedUrl));
        when(fileUploadRepository.save(any(FileUpload.class)))
                .thenReturn(mockFileUpload);

        // when
        String result = fileService.uploadFile(mockFile, FileFolder.COMMUNITY, 1L);

        // then
        assertThat(result).isEqualTo(expectedPresignedUrl);
        verify(s3FileService).uploadFile(mockFile, FileFolder.COMMUNITY, true);
        verify(s3FileService).getPresignedUrl(expectedFileKey);
        verify(fileUploadRepository).save(any(FileUpload.class));
    }

    @Test
    @DisplayName("Presigned URL 관련 테스트들")
    void presignedUrl_Tests() throws Exception {
        String fileKey = "test-images/2024/01/15/uuid-test-image.jpg";
        String expectedUrl = "https://bucket.s3.amazonaws.com/test-images/2024/01/15/uuid-test-image.jpg";
        int expirationMinutes = 10;

        // 1. 일반 Presigned URL 생성 성공
        when(s3FileService.getPresignedUrl(fileKey, expirationMinutes))
                .thenReturn(new URL(expectedUrl));

        URL result = fileService.getPresignedUrl(fileKey, expirationMinutes);
        assertThat(result.toString()).isEqualTo(expectedUrl);
        verify(s3FileService).getPresignedUrl(fileKey, expirationMinutes);

        // 2. 영구 파일 Presigned URL 생성 성공
        when(fileUploadRepository.findByS3Key(fileKey))
                .thenReturn(Optional.of(permanentFile));
        when(s3FileService.getPresignedUrl(fileKey, expirationMinutes))
                .thenReturn(new URL(expectedUrl));

        result = fileService.getPermanentFilePresignedUrl(fileKey, expirationMinutes);
        assertThat(result.toString()).isEqualTo(expectedUrl);
        verify(fileUploadRepository).findByS3Key(fileKey);

        // 3. 임시 파일에 대한 영구 파일 Presigned URL 요청 실패
        when(fileUploadRepository.findByS3Key(fileKey))
                .thenReturn(Optional.of(tempFile));

        assertThatThrownBy(() -> fileService.getPermanentFilePresignedUrl(fileKey, expirationMinutes))
                .isInstanceOf(ImageException.class);

        // 4. 존재하지 않는 파일에 대한 영구 파일 Presigned URL 요청 실패
        when(fileUploadRepository.findByS3Key(fileKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getPermanentFilePresignedUrl(fileKey, expirationMinutes))
                .isInstanceOf(ImageException.class);
    }

    @Test
    @DisplayName("파일 삭제 성공 테스트")
    void deleteFile_Success() throws Exception {
        // given
        String fileKey = "test-images/2024/01/15/uuid-test-image.jpg";
        doNothing().when(s3FileService).deleteFile(fileKey);

        // when
        fileService.deleteFile(fileKey);

        // then
        verify(s3FileService).deleteFile(fileKey);
    }

    @Test
    @DisplayName("게시글 작성 후 파일 처리 테스트들")
    void handleImagesAfterPostCreate_Tests() throws Exception {
        Long userId = 1L;
        
        // 1. 사용된 파일과 사용되지 않은 파일이 모두 있는 경우
        List<FileUpload> tempFiles = Arrays.asList(
                FileUpload.builder()
                        .id(1L)
                        .userId(userId)
                        .s3Key("file1-key")
                        .presignedUrl("url1")
                        .isTemp(true)
                        .build(),
                FileUpload.builder()
                        .id(2L)
                        .userId(userId)
                        .s3Key("file2-key")
                        .presignedUrl("url2")
                        .isTemp(true)
                        .build()
        );
        List<String> usedUrls = Arrays.asList("url1");
        when(fileUploadRepository.findByUserIdAndIsTempTrue(userId))
                .thenReturn(tempFiles);
        doNothing().when(s3FileService).deleteFile(anyString());

        fileService.handleImagesAfterPostCreate(usedUrls, userId);

        verify(fileUploadRepository).findByUserIdAndIsTempTrue(userId);
        verify(s3FileService).deleteFile("file2-key");
        assertThat(tempFiles.get(0).isTemp()).isFalse();
        assertThat(tempFiles.get(1).isTemp()).isTrue();

        // 2. 모든 파일이 사용된 경우
        reset(fileUploadRepository, s3FileService);
        tempFiles = Arrays.asList(
                FileUpload.builder()
                        .id(1L)
                        .userId(userId)
                        .s3Key("file1-key")
                        .presignedUrl("url1")
                        .isTemp(true)
                        .build(),
                FileUpload.builder()
                        .id(2L)
                        .userId(userId)
                        .s3Key("file2-key")
                        .presignedUrl("url2")
                        .isTemp(true)
                        .build()
        );
        when(fileUploadRepository.findByUserIdAndIsTempTrue(userId))
                .thenReturn(tempFiles);
        List<String> allUsedUrls = Arrays.asList("url1", "url2");
        fileService.handleImagesAfterPostCreate(allUsedUrls, userId);
        verify(s3FileService, never()).deleteFile(anyString());
        assertThat(tempFiles.get(0).isTemp()).isFalse();
        assertThat(tempFiles.get(1).isTemp()).isFalse();

        // 3. 사용된 파일이 없는 경우
        reset(fileUploadRepository, s3FileService);
        tempFiles = Arrays.asList(
                FileUpload.builder()
                        .id(1L)
                        .userId(userId)
                        .s3Key("file1-key")
                        .presignedUrl("url1")
                        .isTemp(true)
                        .build(),
                FileUpload.builder()
                        .id(2L)
                        .userId(userId)
                        .s3Key("file2-key")
                        .presignedUrl("url2")
                        .isTemp(true)
                        .build()
        );
        when(fileUploadRepository.findByUserIdAndIsTempTrue(userId))
                .thenReturn(tempFiles);
        fileService.handleImagesAfterPostCreate(Collections.emptyList(), userId);
        verify(s3FileService, times(2)).deleteFile(anyString());
        assertThat(tempFiles.get(0).isTemp()).isTrue();
        assertThat(tempFiles.get(1).isTemp()).isTrue();
    }
} 