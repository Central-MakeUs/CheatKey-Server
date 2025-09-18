package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityReportedComment;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityReportedCommentRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentReportRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    private CommunityCommentRepository commentRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CommunityReportedCommentRepository communityReportedCommentRepository;

    @Autowired
    private AuthRepository authRepository;

    @Test
    @DisplayName("댓글 작성/조회/삭제 end-to-end 성공")
    void commentEndToEnd_success() throws Exception {
        // given: 게시글 생성
        CommunityPost post = CommunityPost.builder()
                .title("제목")
                .content("내용")
                .authorId(1L)
                .authorNickname("테스트유저1")
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
                .authorId(1L)
                .authorNickname("테스트유저1")
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

    @Test
    @DisplayName("댓글 신고 통합 테스트 - 성공 및 중복 신고 예외")
    void reportComment_success_and_duplicate() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("신고 테스트 제목")
                .content("신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(20L)
                .authorNickname("댓글작성자")
                .content("신고당할 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        comment = commentRepository.save(comment);

        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("FAKE");

        // when: 댓글 신고
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: 1회 신고 후 상태 확인 (아직 ACTIVE)
        CommunityComment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(updatedComment.getStatus()).isEqualTo(CommentStatus.ACTIVE);

        // when: 같은 댓글을 다시 신고 시도
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_ALREADY_REPORTED"));
    }

    @Test
    @DisplayName("댓글 신고 실패 - 본인 댓글 신고")
    void reportComment_fail_ownComment() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("신고 테스트 제목")
                .content("신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(1L) // 신고자와 동일한 작성자
                .authorNickname("댓글작성자")
                .content("본인 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        comment = commentRepository.save(comment);

        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("FAKE");

        // when & then: 본인 댓글 신고 시도
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_REPORT_OWN_COMMENT"));
    }

    @Test
    @DisplayName("댓글 신고 실패 - 존재하지 않는 댓글")
    void reportComment_fail_commentNotFound() throws Exception {
        // given
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("FAKE");

        // when & then: 존재하지 않는 댓글 신고 시도
        mockMvc.perform(post("/v1/api/community/comments/99999/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 신고 사유 조회 성공")
    void getCommentReportReasons_success() throws Exception {
        // given
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then: 신고 사유 조회
        mockMvc.perform(get("/v1/api/community/comments/report-reasons")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportCodeList").isArray())
                .andExpect(jsonPath("$.reportCodeList[0].code").exists())
                .andExpect(jsonPath("$.reportCodeList[0].name").exists());
    }

    @Test
    @DisplayName("신고된 댓글 조회 시 표시 확인")
    void getCommentsWithReportedComment() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("신고 테스트 제목")
                .content("신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(20L)
                .authorNickname("댓글작성자")
                .content("신고당할 댓글")
                .status(CommentStatus.REPORTED) // 신고된 상태로 생성
                .build();
        comment = commentRepository.save(comment);

        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);

        // when & then: 댓글 조회 시 신고된 댓글 표시 확인
        mockMvc.perform(get("/v1/api/community/posts/" + post.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorNickname").value("(신고된 유저)"))
                .andExpect(jsonPath("$[0].content").value("관리자 규제된 댓글입니다."))
                .andExpect(jsonPath("$[0].status").value("REPORTED"))
                .andExpect(jsonPath("$[0].canDelete").value(false));
    }

    @Test
    @DisplayName("2회 신고 시 댓글 상태 변경 통합 테스트")
    void reportComment_twoReportsChangeStatus() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("2회 신고 테스트 제목")
                .content("2회 신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(20L)
                .authorNickname("댓글작성자")
                .content("2회 신고당할 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        comment = commentRepository.save(comment);

        // 이미 1회 신고된 데이터를 미리 생성
        CommunityReportedComment firstReport = CommunityReportedComment.builder()
                .commentId(comment.getId())
                .reporterId(2L)
                .reasonCode("FAKE")
                .build();
        communityReportedCommentRepository.save(firstReport);

        // JWT 하나만 생성
        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("HATE");

        // when: 두 번째 신고 (2회째)
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: 2회 신고 후 상태 확인 (REPORTED로 변경됨)
        CommunityComment afterSecondReport = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(afterSecondReport.getStatus()).isEqualTo(CommentStatus.REPORTED);

        // when: 댓글 조회 시 신고된 댓글 표시 확인
        mockMvc.perform(get("/v1/api/community/posts/" + post.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorNickname").value("(신고된 유저)"))
                .andExpect(jsonPath("$[0].content").value("관리자 규제된 댓글입니다."))
                .andExpect(jsonPath("$[0].status").value("REPORTED"))
                .andExpect(jsonPath("$[0].canDelete").value(false));
    }

    @Test
    @DisplayName("1회 신고 시 댓글 상태 변경 안됨 통합 테스트")
    void reportComment_oneReportNoStatusChange() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("1회 신고 테스트 제목")
                .content("1회 신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(20L)
                .authorNickname("댓글작성자")
                .content("1회 신고당할 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        comment = commentRepository.save(comment);

        String jwt = jwtProvider.createAccessToken(1L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("FAKE");

        // when: 1회 신고
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: 1회 신고 후 상태 확인 (여전히 ACTIVE)
        CommunityComment afterReport = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(afterReport.getStatus()).isEqualTo(CommentStatus.ACTIVE);

        // when: 댓글 조회 시 정상 댓글 표시 확인
        mockMvc.perform(get("/v1/api/community/posts/" + post.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorNickname").value("댓글작성자"))
                .andExpect(jsonPath("$[0].content").value("1회 신고당할 댓글"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }


    @Test
    @DisplayName("3회 신고 시 상태 변경 안됨 통합 테스트")
    void reportComment_threeReportsNoStatusChange() throws Exception {
        // given: 게시글과 댓글 생성
        CommunityPost post = CommunityPost.builder()
                .title("3회 신고 테스트 제목")
                .content("3회 신고 테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(10L)
                .authorNickname("테스트유저1")
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
        post = postRepository.save(post);

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(null)
                .authorId(20L)
                .authorNickname("댓글작성자")
                .content("3회 신고당할 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        comment = commentRepository.save(comment);

        // 이미 2회 신고된 데이터를 미리 생성 (이미 REPORTED 상태)
        CommunityReportedComment firstReport = CommunityReportedComment.builder()
                .commentId(comment.getId())
                .reporterId(1L)
                .reasonCode("FAKE")
                .build();
        communityReportedCommentRepository.save(firstReport);

        CommunityReportedComment secondReport = CommunityReportedComment.builder()
                .commentId(comment.getId())
                .reporterId(2L)
                .reasonCode("HATE")
                .build();
        communityReportedCommentRepository.save(secondReport);

        // 댓글 상태를 REPORTED로 변경 (2회 신고로 인한 상태 변경)
        comment.setStatus(CommentStatus.REPORTED);
        commentRepository.save(comment);

        // JWT 하나만 생성
        String jwt = jwtProvider.createAccessToken(3L, Provider.KAKAO, AuthRole.USER);
        CommunityCommentReportRequest request = new CommunityCommentReportRequest();
        request.setReasonCode("SPAM");

        // when: 세 번째 신고 (3회째)
        mockMvc.perform(post("/v1/api/community/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then: 3회 신고 후 상태 확인 (여전히 REPORTED)
        CommunityComment afterThirdReport = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(afterThirdReport.getStatus()).isEqualTo(CommentStatus.REPORTED);

        // when: 댓글 조회 시 신고된 댓글 표시 확인
        mockMvc.perform(get("/v1/api/community/posts/" + post.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorNickname").value("(신고된 유저)"))
                .andExpect(jsonPath("$[0].content").value("관리자 규제된 댓글입니다."))
                .andExpect(jsonPath("$[0].status").value("REPORTED"))
                .andExpect(jsonPath("$[0].canDelete").value(false));
    }
}