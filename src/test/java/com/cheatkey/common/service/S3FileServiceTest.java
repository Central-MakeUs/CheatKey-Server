package com.cheatkey.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.file.domain.entity.FileFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private S3FileService s3FileService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile largeFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile unsupportedFile;
    private MockMultipartFile nullFilenameFile;

    @BeforeEach
    void setUp() {
        // 테스트용 설정
        ReflectionTestUtils.setField(s3FileService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3FileService, "presignedUrlExpiration", 10);

        // 유효한 이미지 파일
        validImageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 큰 파일 (5MB 초과)
        largeFile = new MockMultipartFile(
                "file",
                "large-image.jpg",
                "image/jpeg",
                new byte[6 * 1024 * 1024] // 6MB
        );

        // 빈 파일
        emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // 지원하지 않는 파일
        unsupportedFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // 파일명이 null인 파일
        nullFilenameFile = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                "test content".getBytes()
        );
    }

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_Success() throws Exception {
        // given
        when(amazonS3.putObject(any())).thenReturn(null);

        // when
        String result = s3FileService.uploadFile(validImageFile, FileFolder.COMMUNITY, 1L, true);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("community");
        verify(amazonS3).putObject(any());
    }

    @Test
    @DisplayName("파일 검증 실패 테스트들")
    void uploadFile_ValidationFailures() {
        // null 파일
        assertThatThrownBy(() -> s3FileService.uploadFile(null, FileFolder.COMMUNITY, 1L, true))
                .isInstanceOf(ImageException.class);

        // 빈 파일
        assertThatThrownBy(() -> s3FileService.uploadFile(emptyFile, FileFolder.COMMUNITY, 1L, true))
                .isInstanceOf(ImageException.class);

        // 큰 파일
        assertThatThrownBy(() -> s3FileService.uploadFile(largeFile, FileFolder.COMMUNITY, 1L, true))
                .isInstanceOf(ImageException.class);

        // 지원하지 않는 파일 형식
        assertThatThrownBy(() -> s3FileService.uploadFile(unsupportedFile, FileFolder.COMMUNITY, 1L, true))
                .isInstanceOf(ImageException.class);

        // 파일명이 null인 파일
        assertThatThrownBy(() -> s3FileService.uploadFile(nullFilenameFile, FileFolder.COMMUNITY, 1L, true))
                .isInstanceOf(ImageException.class);

        // S3 호출이 없어야 함
        verify(amazonS3, never()).putObject(any());
    }

    @Test
    @DisplayName("다양한 파일 형식 업로드 성공 테스트")
    void uploadFile_VariousFormats_Success() throws Exception {
        // given
        when(amazonS3.putObject(any())).thenReturn(null);

        // 이미지 파일들
        String[] imageFormats = {"jpg", "jpeg", "png", "webp"};
        String[] contentTypes = {"image/jpeg", "image/jpeg", "image/png", "image/webp"};
        
        for (int i = 0; i < imageFormats.length; i++) {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file",
                    "test-image." + imageFormats[i],
                    contentTypes[i],
                    "test image content".getBytes()
            );

            // when
            String result = s3FileService.uploadFile(imageFile, FileFolder.COMMUNITY, 1L, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("community");
        }

        // PDF 파일
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        String result = s3FileService.uploadFile(pdfFile, FileFolder.COMMUNITY, 1L, true);
        assertThat(result).isNotNull();
        assertThat(result).contains("community");

        // 총 5번의 putObject 호출 (4개 이미지 + 1개 PDF)
        verify(amazonS3, times(5)).putObject(any());
    }

    @Test
    @DisplayName("Presigned URL 생성 성공 테스트")
    void getPresignedUrl_Success() throws Exception {
        // given
        String fileKey = "test-images/2024/01/15/uuid-test-image.jpg";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-images/2024/01/15/uuid-test-image.jpg";
        int expirationMinutes = 10;
        
        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL(expectedUrl));

        // when
        URL result = s3FileService.getPresignedUrl(fileKey, expirationMinutes);

        // then
        assertThat(result.toString()).isEqualTo(expectedUrl);
        verify(amazonS3).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    @DisplayName("기본 만료 시간으로 Presigned URL 생성 성공 테스트")
    void getPresignedUrl_DefaultExpiration_Success() throws Exception {
        // given
        String fileKey = "test-images/2024/01/15/uuid-test-image.jpg";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-images/2024/01/15/uuid-test-image.jpg";
        
        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL(expectedUrl));

        // when
        URL result = s3FileService.getPresignedUrl(fileKey);

        // then
        assertThat(result.toString()).isEqualTo(expectedUrl);
        verify(amazonS3).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    @DisplayName("파일 삭제 테스트들")
    void deleteFile_Tests() throws Exception {
        String fileKey = "test-images/2024/01/15/uuid-test-image.jpg";

        // 파일 존재하는 경우 삭제 성공
        when(amazonS3.doesObjectExist("test-bucket", fileKey)).thenReturn(true);
        doNothing().when(amazonS3).deleteObject("test-bucket", fileKey);

        s3FileService.deleteFile(fileKey);

        verify(amazonS3).doesObjectExist("test-bucket", fileKey);
        verify(amazonS3).deleteObject("test-bucket", fileKey);

        // 파일 존재하지 않는 경우 (삭제하지 않음)
        reset(amazonS3);
        when(amazonS3.doesObjectExist("test-bucket", fileKey)).thenReturn(false);

        s3FileService.deleteFile(fileKey);

        verify(amazonS3).doesObjectExist("test-bucket", fileKey);
        verify(amazonS3, never()).deleteObject(anyString(), anyString());
    }
} 