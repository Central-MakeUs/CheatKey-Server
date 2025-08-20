package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostReportRequest;
import com.cheatkey.module.community.interfaces.dto.CommunityPostBlockRequest;
import com.cheatkey.module.community.domain.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
    private AuthRepository authRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CommunityReportedPostRepository communityReportedPostRepository;
    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    private Long testUserId;
    private String testUserNickname;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Auth testUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("test-user-" + System.currentTimeMillis())
                .email("test@test.com")
                .nickname("테스트유저")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        testUser = authRepository.save(testUser);
        testUserId = testUser.getId();
        testUserNickname = testUser.getNickname();
    }

    @Test
    @DisplayName("커뮤니티 글 작성 통합 테스트 - 성공 및 DB 저장 검증")
    void createPost_success() throws Exception {
        // given
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .build();
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

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
        assertThat(post.getAuthorId()).isEqualTo(testUserId);
        assertThat(post.getAuthorNickname()).isEqualTo(testUserNickname);
    }

    @Test
    @DisplayName("내용이 너무 짧으면 400 반환")
    void createPost_contentTooShort() throws Exception {
        CommunityPostCreateRequest request = CommunityPostCreateRequest.builder()
                .title("통합테스트 제목12345")
                .content("짧음") // 10자 미만
                .category(CommunityCategory.REPORT)
                .build();
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

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
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(null)
                .build();
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

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
                .title("통합테스트 제목12345")
                .content("통합테스트 내용12345")
                .category(CommunityCategory.REPORT)
                .fileUploadIds(java.util.Arrays.asList(1L,2L,3L,4L,5L,6L))
                .build();
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

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
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(com.cheatkey.module.community.domain.entity.PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);
        Long postId = post.getId();
        CommunityPostReportRequest request = new CommunityPostReportRequest();
        request.setReasonCode("FAKE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

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
    @DisplayName("게시글 작성자 차단 통합 테스트 - 성공 및 중복 차단 예외")
    void blockPostAuthor_success_and_duplicate() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("차단 테스트 제목")
                .content("차단 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(40L)  // 차단당할 작성자
                .authorNickname("차단당할유저")
                .viewCount(0L)
                .status(com.cheatkey.module.community.domain.entity.PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);
        Long postId = post.getId();
        
        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (정상 차단)
        mockMvc.perform(post("/v1/api/community/posts/" + postId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then (중복 차단)
        mockMvc.perform(post("/v1/api/community/posts/" + postId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_BLOCKED"));
    }

    @Test
    @DisplayName("게시글 작성자 차단 통합 테스트 - 존재하지 않는 게시글")
    void blockPostAuthor_postNotFound() throws Exception {
        // given
        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (존재하지 않는 게시글)
        mockMvc.perform(post("/v1/api/community/posts/99999/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("게시글 작성자 차단 통합 테스트 - 자기 자신 차단 시도")
    void blockPostAuthor_cannotBlockSelf() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("자기 차단 테스트 제목")
                .content("자기 차단 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(testUserId)  // 현재 로그인한 유저가 작성자
                .authorNickname("테스트유저")
                .viewCount(0L)
                .status(com.cheatkey.module.community.domain.entity.PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);
        Long postId = post.getId();
        
        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (자기 자신 차단 시도)
        mockMvc.perform(post("/v1/api/community/posts/" + postId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
    }
} 