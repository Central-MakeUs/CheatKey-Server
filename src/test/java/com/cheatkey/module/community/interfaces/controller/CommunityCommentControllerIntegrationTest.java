package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.CommunityPostBlockRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityCommentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityCommentRepository communityCommentRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private JwtProvider jwtProvider;

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
    @DisplayName("댓글 작성자 차단 통합 테스트 - 성공 및 중복 차단 예외")
    void blockCommentAuthor_success_and_duplicate() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(testUserId)
                .authorNickname(testUserNickname)
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(40L)  // 차단당할 작성자
                .authorNickname("차단당할유저")
                .content("차단 테스트 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        communityCommentRepository.save(comment);
        Long commentId = comment.getId();

        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (정상 차단)
        mockMvc.perform(post("/v1/api/community/comments/" + commentId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then (중복 차단)
        mockMvc.perform(post("/v1/api/community/comments/" + commentId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_BLOCKED"));
    }

    @Test
    @DisplayName("댓글 작성자 차단 통합 테스트 - 존재하지 않는 댓글")
    void blockCommentAuthor_commentNotFound() throws Exception {
        // given
        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (존재하지 않는 댓글)
        mockMvc.perform(post("/v1/api/community/comments/99999/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 작성자 차단 통합 테스트 - 자기 자신 차단 시도")
    void blockCommentAuthor_cannotBlockSelf() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(testUserId)
                .authorNickname(testUserNickname)
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(testUserId)  // 현재 로그인한 유저가 작성자
                .authorNickname(testUserNickname)
                .content("내 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        communityCommentRepository.save(comment);
        Long commentId = comment.getId();

        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("HATE");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (자기 자신 차단 시도)
        mockMvc.perform(post("/v1/api/community/comments/" + commentId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
    }

    @Test
    @DisplayName("대댓글 작성자 차단 통합 테스트 - 성공")
    void blockReplyAuthor_success() throws Exception {
        // given
        CommunityPost post = CommunityPost.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(testUserId)
                .authorNickname(testUserNickname)
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(post);

        // 부모 댓글
        CommunityComment parentComment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(testUserId)
                .authorNickname(testUserNickname)
                .content("부모 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        communityCommentRepository.save(parentComment);

        // 대댓글
        CommunityComment reply = CommunityComment.builder()
                .post(post)
                .parent(parentComment)
                .authorId(50L)  // 차단당할 대댓글 작성자
                .authorNickname("차단당할대댓글작성자")
                .content("차단 테스트 대댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        communityCommentRepository.save(reply);
        Long replyId = reply.getId();

        CommunityPostBlockRequest request = new CommunityPostBlockRequest();
        request.setReason("SPAM");
        String jwt = jwtProvider.createAccessToken(testUserId, Provider.KAKAO, AuthRole.USER);

        // when & then (대댓글 작성자 차단)
        mockMvc.perform(post("/v1/api/community/comments/" + replyId + "/author/block")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
