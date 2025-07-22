package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.community.domian.entity.CommunityCategory;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("커뮤니티 글 작성 통합 테스트 - 성공 및 DB 저장 검증")
    void createPost_success() throws Exception {
        // given
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .build();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then
        String response = mockMvc.perform(post("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long postId = Long.valueOf(response);
        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        assertThat(post).isNotNull();
        assertThat(post.getTitle()).isEqualTo("통합테스트 제목12345");
        assertThat(post.getContent()).isEqualTo("통합테스트 내용12345");
        assertThat(post.getCategory()).isEqualTo(CommunityCategory.REPORT);
        assertThat(post.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("제목이 너무 짧으면 400 반환")
    void createPost_titleTooShort() throws Exception {
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("짧음") // 10자 미만
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .build();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        mockMvc.perform(post("/v1/api/community/posts")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내용이 너무 짧으면 400 반환")
    void createPost_contentTooShort() throws Exception {
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("통합테스트 제목12345")
                .content("짧음") // 10자 미만
                .category(CommunityCategory.REPORT)
                .build();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        mockMvc.perform(post("/v1/api/community/posts")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카테고리가 null이면 400 반환")
    void createPost_categoryNull() throws Exception {
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(null)
                .build();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        mockMvc.perform(post("/v1/api/community/posts")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파일 첨부가 6개 이상이면 400 반환")
    void createPost_fileUploadIdsTooMany() throws Exception {
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .fileUploadIds(java.util.Arrays.asList(1L,2L,3L,4L,5L,6L))
                .build();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        mockMvc.perform(post("/v1/api/community/posts")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 