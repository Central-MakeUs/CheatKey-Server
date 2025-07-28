package com.cheatkey.module.file;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import com.cheatkey.module.file.domain.entity.FileFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("S3 파일 업로드 실 API 호출용 테스트 - 수동 실행 전용")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private S3FileService s3FileService;

    @Autowired
    private JwtProvider jwtProvider;

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
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then
        String response = mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file)
                        .param("folder", FileFolder.TEST.name())
                        .param("userId", "1")
                        .param("isTemp", "true")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].originalName").value("test-image.jpg"))
                .andExpect(jsonPath("$[0].isTemp").value(true))
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
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file1)
                        .file(file2)
                        .param("folderType", FileFolder.TEST.name())
                        .param("userId", "1")
                        .param("isTemp", "true")
                        .header("Authorization", "Bearer " + jwt))
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
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(emptyFile)
                        .param("folder", FileFolder.TEST.name())
                        .param("userId", "1")
                        .param("isTemp", "true")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest()); // 실제로는 400 에러 반환
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
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(unsupportedFile)
                        .param("folder", FileFolder.TEST.name())
                        .param("userId", "1")
                        .param("isTemp", "true")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest()); // 실제로는 400 에러 반환
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
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // 파일 업로드
        mockMvc.perform(multipart("/v1/api/files/upload")
                        .file(file)
                        .param("folder", FileFolder.TEST.name())
                        .param("userId", "1")
                        .param("isTemp", "true")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // 업로드된 파일의 키 가져오기
        List<FileUpload> uploadedFiles = fileUploadRepository.findByUserIdAndIsTempTrue(1L);
        assertThat(uploadedFiles).isNotEmpty();
        String fileKey = uploadedFiles.get(0).getS3Key();

        // when & then
        mockMvc.perform(delete("/v1/api/files/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileKey\":\"" + fileKey + "\"}")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }
}

