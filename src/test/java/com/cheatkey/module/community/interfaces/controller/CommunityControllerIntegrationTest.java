package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostReportRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostBlockRequest;
import com.cheatkey.module.community.domain.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CommunityReportedPostRepository communityReportedPostRepository;
    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    @Test
    @DisplayName("커뮤니티 글 작성 통합 테스트 - 성공 및 DB 저장 검증")
    void createPost_success() throws Exception {
        // given
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .userId(1L)
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .nickname("테스트유저1")
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

    @Test
    @DisplayName("게시글 신고 통합 테스트 - 성공 및 중복 신고 예외")
    void reportPost_success_and_duplicate() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("신고 테스트 제목")
                .content("신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .userId(10L)
                .nickname("테스트유저1")
                .viewCount(0L)
                .status(com.cheatkey.module.community.domain.entity.PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);
        Long postId = post.getId();
        CommunityPostReportRequest request = new CommunityPostReportRequest();
        request.setReasonCode("FAKE");
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then (정상 신고)
        mockMvc.perform(post("/v1/api/community/posts/" + postId + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then (중복 신고)
        mockMvc.perform(post("/v1/api/community/posts/" + postId + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_REPORTED"));
    }

    @Test
    @DisplayName("게시글 차단 통합 테스트 - 성공 및 중복 차단 예외")
    void blockUser_success_and_duplicate() throws Exception {
        // given
        Long blockerId = 30L;
        Long blockedId = 40L;
        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then (정상 차단)
        mockMvc.perform(post("/v1/api/community/users/" + blockedId + "/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then (중복 차단)
        mockMvc.perform(post("/v1/api/community/users/" + blockedId + "/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_BLOCKED"));
    }
} 