package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CommunityCommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("댓글 작성/조회/삭제 end-to-end 성공")
    void commentEndToEnd_success() throws Exception {
        // given: 게시글 생성
        CommunityPost post = CommunityPost.builder()
                .title("제목")
                .content("내용")
                .userId(1L)
                .nickname("테스트유저1")
                .viewCount(0L)
                .category(CommunityCategory.TALK)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when: 댓글 작성
        CommunityCommentRequest commentReq = new CommunityCommentRequest();
        commentReq.setPostId(post.getId());
        commentReq.setContent("댓글1");
        String commentJson = objectMapper.writeValueAsString(commentReq);

        String commentId = mockMvc.perform(post("/v1/api/community/comments")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // when: 대댓글 작성
        CommunityCommentRequest replyReq = new CommunityCommentRequest();
        replyReq.setPostId(post.getId());
        replyReq.setParentId(Long.valueOf(commentId));
        replyReq.setContent("대댓글1");
        String replyJson = objectMapper.writeValueAsString(replyReq);

        mockMvc.perform(post("/v1/api/community/comments")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyJson))
                .andExpect(status().isOk());

        // then: 댓글/대댓글 조회
        mockMvc.perform(get("/v1/api/community/posts/" + post.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("댓글1"))
                .andExpect(jsonPath("$[0].children[0].content").value("대댓글1"));

        // when: 댓글 삭제
        mockMvc.perform(delete("/v1/api/community/comments/" + commentId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("삭제/신고된 게시글에 댓글 작성 실패")
    void createComment_fail_deletedOrReportedPost() throws Exception {
        // given: 삭제 상태 게시글 생성
        CommunityPost post = CommunityPost.builder()
                .title("삭제된글")
                .content("내용")
                .userId(1L)
                .nickname("테스트유저1")
                .viewCount(0L)
                .category(CommunityCategory.TALK)
                .status(PostStatus.DELETED)
                .build();
        post = postRepository.save(post);
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when: 댓글 작성 시도
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(post.getId());
        req.setContent("댓글 내용");
        String json = objectMapper.writeValueAsString(req);

        // then: 실패 응답
        mockMvc.perform(post("/v1/api/community/comments")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
} 