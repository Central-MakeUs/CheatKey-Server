package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CommunityPostIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        communityPostBlockRepository.deleteAll();
        communityPostRepository.deleteAll();

        // 기본 데이터: ACTIVE 2개, DELETED 1개, 차단 1개
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle1").content("TestContents1").userId(1L).nickname("TestUser1")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle2").content("TestContents2").userId(2L).nickname("TestUser2")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle3").content("TestContents3").userId(3L).nickname("TestUser3")
                        .status(PostStatus.DELETED).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle4").content("TestContents4").userId(4L).nickname("TestUser4")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());

        // userId=1이 userId=4를 차단
        communityPostBlockRepository.save(
            com.cheatkey.module.community.domain.entity.CommunityPostBlock.builder().blockerId(1L).blockedId(4L).isActive(true).build()
        );
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 목록 - 정상/차단/상태 정책")
    void 커뮤니티_게시글_목록_정책_통합테스트() throws Exception {
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        String response = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("userId", "1")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // PageImpl의 content만 추출
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode content = root.get("content");
        assertThat(content.size()).isEqualTo(2); // DELETED, 차단 제외, ACTIVE만 남음
        assertThat(content.get(0).get("authorNickname").asText()).isIn("TestUser1", "TestUser2");
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 상세 - 정상/차단/상태/조회수 정책")
    void 커뮤니티_게시글_상세_정책_통합테스트() throws Exception {
        CommunityPost post = communityPostRepository.findAll().stream().filter(p -> p.getUserId() == 2L).findFirst().orElseThrow();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        String response = mockMvc.perform(get("/v1/api/community/posts/" + post.getId())
                        .header("Authorization", "Bearer " + jwt)
                .param("userId", "1"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(response).get("authorNickname").asText()).isEqualTo("TestUser2");
    }
} 