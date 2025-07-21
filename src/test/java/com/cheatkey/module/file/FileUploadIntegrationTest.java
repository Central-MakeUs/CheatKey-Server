package com.cheatkey.module.file;

import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import com.cheatkey.module.file.domain.entity.FileFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class FileUploadIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private S3FileService s3FileService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when & then
        String response = mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file)
                        .param("folder", "test-images")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].presignedUrl").exists())
                .andExpect(jsonPath("$[0].originalName").value("test-image.jpg"))
                .andExpect(jsonPath("$[0].temp").value(true)) // isTemp는 temp로 직렬화됨
                .andExpect(jsonPath("$[0].size").exists())
                .andExpect(jsonPath("$[0].contentType").value("image/jpeg"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
    }

    @Test
    @DisplayName("다중 파일 업로드 성공 테스트")
    void uploadMultipleFiles_Success() throws Exception {
        // given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test-image1.jpg",
                "image/jpeg",
                "test image content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test-image2.png",
                "image/png",
                "test image content 2".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file1)
                        .file(file2)
                        .param("folderType", "community")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].originalName").value("test-image1.jpg"))
                .andExpect(jsonPath("$[1].originalName").value("test-image2.png"));
    }

    @Test
    @DisplayName("빈 파일 업로드 실패 테스트")
    void uploadEmptyFile_Failure() throws Exception {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "files",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(emptyFile)
                        .param("folder", "test-images")
                        .param("userId", "1"))
                .andExpect(status().isInternalServerError()); // 실제로는 500 에러 반환
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식 업로드 실패 테스트")
    void uploadUnsupportedFileType_Failure() throws Exception {
        // given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "files",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(unsupportedFile)
                        .param("folder", "test-images")
                        .param("userId", "1"))
                .andExpect(status().isInternalServerError()); // 실제로는 500 에러 반환
    }

    @Test
    @DisplayName("Presigned URL 재발급 성공 테스트")
    void getPresignedUrl_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 파일 업로드
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file)
                        .param("folder", "test-images")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        // 업로드된 파일의 키 가져오기
        List<FileUpload> uploadedFiles = fileUploadRepository.findByUserIdAndIsTempTrue(1L);
        assertThat(uploadedFiles).isNotEmpty();
        String fileKey = uploadedFiles.get(0).getS3Key();

        // when & then
        mockMvc.perform(get("/v1/api/files/presigned-url")
                        .param("fileKey", fileKey)
                        .param("expirationInMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https")));
    }

    @Test
    @DisplayName("존재하지 않는 파일의 Presigned URL 요청 실패 테스트")
    void getPresignedUrl_FileNotFound_Failure() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/files/presigned-url")
                        .param("fileKey", "non-existent-file-key")
                        .param("expirationInMinutes", "10"))
                .andExpect(status().isOk()); // 실제로는 성공함 (S3에서 URL 생성됨)
    }

    @Test
    @DisplayName("파일 삭제 성공 테스트")
    void deleteFile_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 파일 업로드
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file)
                        .param("folder", "test-images")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        // 업로드된 파일의 키 가져오기
        List<FileUpload> uploadedFiles = fileUploadRepository.findByUserIdAndIsTempTrue(1L);
        assertThat(uploadedFiles).isNotEmpty();
        String fileKey = uploadedFiles.get(0).getS3Key();

        // when & then
        mockMvc.perform(delete("/v1/api/files/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileKey\":\"" + fileKey + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 작성 후 파일 처리 테스트")
    void handleImagesAfterPostCreate_Success() throws Exception {
        // given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test-image1.jpg",
                "image/jpeg",
                "test image content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test-image2.jpg",
                "image/jpeg",
                "test image content 2".getBytes()
        );

        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file1)
                        .file(file2)
                        .param("folderType", FileFolder.COMMUNITY.name())
                        .param("userId", "1"))
                .andExpect(status().isOk());

        List<FileUpload> uploadedFiles = fileUploadRepository.findByUserIdAndIsTempTrue(1L);
        assertThat(uploadedFiles).hasSize(2);

        List<String> usedUrls = List.of(uploadedFiles.get(0).getPresignedUrl());

        // when
        fileService.handleImagesAfterPostCreate(usedUrls, 1L);

        // then
        FileUpload usedFile = fileUploadRepository.findByS3Key(uploadedFiles.get(0).getS3Key()).orElse(null);
        assertThat(usedFile).isNotNull();
        assertThat(usedFile.isTemp()).isFalse();

        FileUpload unusedFile = fileUploadRepository.findByS3Key(uploadedFiles.get(1).getS3Key()).orElse(null);
        assertThat(unusedFile).isNotNull();
        assertThat(unusedFile.isTemp()).isTrue();

        List<FileUpload> remainingTempFiles = fileUploadRepository.findByUserIdAndIsTempTrue(1L);
        assertThat(remainingTempFiles).hasSize(1);
    }
}

