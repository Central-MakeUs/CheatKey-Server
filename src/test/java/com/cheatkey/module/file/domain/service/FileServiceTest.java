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

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
    }

    @Test
    @DisplayName("임시 파일 업로드 성공 테스트")
    void uploadTempFile_Success() throws Exception {
        // given
        String expectedFileKey = "uploads/temp/2024/01/15/uuid-test-image.jpg";
        when(s3FileService.uploadFile(any(MockMultipartFile.class), any(FileFolder.class), anyLong(), anyBoolean()))
                .thenReturn(expectedFileKey);
        when(s3FileService.getPresignedUrl(anyString()))
                .thenReturn(new URL("https://bucket.s3.amazonaws.com/" + expectedFileKey));
        when(fileUploadRepository.save(any(FileUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        FileUpload result = fileService.uploadTempFile(mockFile, 1L);

        // then
        assertThat(result.getS3Key()).isEqualTo(expectedFileKey);
        assertThat(result.getIsTemp()).isTrue();
        verify(s3FileService).uploadFile(mockFile, FileFolder.COMMUNITY, 1L, true);
        verify(fileUploadRepository).save(any(FileUpload.class));
    }

    @Test
    @DisplayName("파일 삭제 성공 테스트")
    void deleteFile_Success() throws Exception {
        // given
        String fileKey = "uploads/temp/2024/01/15/uuid-test-image.jpg";
        doNothing().when(s3FileService).deleteFile(fileKey);

        // when
        fileService.deleteFile(fileKey);

        // then
        verify(s3FileService).deleteFile(fileKey);
    }
} 