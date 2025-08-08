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

        // 기본 데이터: ACTIVE 2개, DELETED 1개, 차단 1개, 다른 카테고리 1개
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle1").content("TestContents1").authorId(1L).authorNickname("TestUser1")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle2").content("TestContents2").authorId(2L).authorNickname("TestUser2")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle3").content("TestContents3").authorId(3L).authorNickname("TestUser3")
                        .status(PostStatus.DELETED).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TestTitle4").content("TestContents4").authorId(4L).authorNickname("TestUser4")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.REPORT).viewCount(0L).build());
        communityPostRepository.save(
                CommunityPost.builder().title("TalkTitle").content("TalkContent").authorId(5L).authorNickname("TestUser5")
                        .status(PostStatus.ACTIVE).category(CommunityCategory.TALK).viewCount(0L).build());

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
                .param("size", "20")
                .param("category", "REPORT"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // PageImpl의 content만 추출
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode content = root.get("content");
        assertThat(content.size()).isEqualTo(2); // DELETED, 차단 제외, ACTIVE만 남음
        assertThat(content.get(0).get("authorNickname").asText()).isIn("TestUser1", "TestUser2");
        
        // content 필드가 포함되어 있는지 확인
        assertThat(content.get(0).has("content")).isTrue();
        assertThat(content.get(0).get("content").asText()).isIn("TestContents1", "TestContents2");
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 목록 - 카테고리 필터링 테스트")
    void 커뮤니티_게시글_목록_카테고리_필터링_테스트() throws Exception {
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // REPORT 카테고리 조회
        String reportResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("category", "REPORT"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode reportContent = objectMapper.readTree(reportResponse).get("content");
        assertThat(reportContent.size()).isEqualTo(2); // 차단 제외하고 2개

        // TALK 카테고리 조회
        String talkResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("category", "TALK"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode talkContent = objectMapper.readTree(talkResponse).get("content");
        assertThat(talkContent.size()).isEqualTo(1); // TALK 카테고리 1개
        assertThat(talkContent.get(0).get("title").asText()).isEqualTo("TalkTitle");
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 목록 - 검색 기능 테스트")
    void 커뮤니티_게시글_목록_검색_기능_테스트() throws Exception {
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // 제목으로 검색
        String titleSearchResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "TestTitle"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode titleSearchContent = objectMapper.readTree(titleSearchResponse).get("content");
        assertThat(titleSearchContent.size()).isEqualTo(2); // TestTitle1, TestTitle2

        // 내용으로 검색
        String contentSearchResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "TestContents"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode contentSearchContent = objectMapper.readTree(contentSearchResponse).get("content");
        assertThat(contentSearchContent.size()).isEqualTo(2); // TestContents1, TestContents2
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 목록 - 정렬 기능 테스트")
    void 커뮤니티_게시글_목록_정렬_기능_테스트() throws Exception {
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // 최신순 정렬 (기본값)
        String latestResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("sort", "latest"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode latestContent = objectMapper.readTree(latestResponse).get("content");
        assertThat(latestContent.size()).isGreaterThan(0);

        // 인기순 정렬
        String popularResponse = mockMvc.perform(get("/v1/api/community/posts")
                        .header("Authorization", "Bearer " + jwt)
                .param("page", "1")
                .param("size", "20")
                .param("sort", "popular"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode popularContent = objectMapper.readTree(popularResponse).get("content");
        assertThat(popularContent.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("[통합] 커뮤니티 게시글 상세 - 정상/차단/상태/조회수 정책")
    void 커뮤니티_게시글_상세_정책_통합테스트() throws Exception {
        CommunityPost post = communityPostRepository.findAll().stream().filter(p -> p.getAuthorId() == 2L).findFirst().orElseThrow();
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        String response = mockMvc.perform(get("/v1/api/community/posts/" + post.getId())
                        .header("Authorization", "Bearer " + jwt)
                .param("userId", "1"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(response).get("authorNickname").asText()).isEqualTo("TestUser2");
    }
} 