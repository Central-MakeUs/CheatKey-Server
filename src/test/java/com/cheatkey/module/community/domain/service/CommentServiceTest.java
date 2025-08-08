package com.cheatkey.module.community.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class CommentServiceTest {
    @Mock
    private CommunityCommentRepository commentRepository;
    @Mock
    private CommunityPostRepository postRepository;
    @Mock
    private CommunityPostBlockRepository communityPostBlockRepository;
    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void createComment_success() {
        CommunityPost post = CommunityPost.builder().id(1L).status(null).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(1L);
        req.setContent("댓글 내용");
        when(commentRepository.save(any())).thenAnswer(inv -> {
            CommunityComment c = inv.getArgument(0);
            return CommunityComment.builder()
                .id(10L)
                .post(c.getPost())
                .parent(c.getParent())
                .authorId(c.getAuthorId())
                .authorNickname(c.getAuthorNickname())
                .content(c.getContent())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .deletedAt(c.getDeletedAt())
                .build();
        });
        Long id = commentService.createComment(req, 2L, "닉네임");
        assertThat(id).isEqualTo(10L);
    }

    @Test
    @DisplayName("삭제/신고된 게시글에 댓글 작성 실패")
    void createComment_fail_deletedOrReportedPost() {
        CommunityPost post = CommunityPost.builder().id(1L).status(PostStatus.DELETED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(1L);
        req.setContent("댓글 내용");
        assertThatThrownBy(() -> commentService.createComment(req, 2L, "닉네임"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_DELETED_OR_REPORTED);
    }

    @Test
    @DisplayName("부모 댓글이 없는 대댓글 작성 실패")
    void createComment_fail_parentNotFound() {
        CommunityPost post = CommunityPost.builder().id(1L).status(null).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(1L);
        req.setParentId(99L);
        req.setContent("대댓글 내용");
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createComment(req, 2L, "닉네임"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_PARENT_NOT_FOUND);
    }

    @Test
    @DisplayName("대댓글의 대댓글 작성 실패")
    void createComment_fail_replyToReply() {
        CommunityPost post = CommunityPost.builder().id(1L).status(null).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        CommunityComment parent = CommunityComment.builder().id(99L).parent(mock(CommunityComment.class)).status(CommentStatus.ACTIVE).build();
        when(commentRepository.findById(99L)).thenReturn(Optional.of(parent));
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(1L);
        req.setParentId(99L);
        req.setContent("대댓글 내용");
        assertThatThrownBy(() -> commentService.createComment(req, 2L, "닉네임"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_CANNOT_REPLY_TO_REPLY);
    }

    @Test
    @DisplayName("삭제된 댓글에 대댓글 작성 실패")
    void createComment_fail_replyToDeleted() {
        CommunityPost post = CommunityPost.builder().id(1L).status(null).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        CommunityComment parent = CommunityComment.builder().id(99L).parent(null).status(CommentStatus.DELETED).build();
        when(commentRepository.findById(99L)).thenReturn(Optional.of(parent));
        CommunityCommentRequest req = new CommunityCommentRequest();
        req.setPostId(1L);
        req.setParentId(99L);
        req.setContent("대댓글 내용");
        assertThatThrownBy(() -> commentService.createComment(req, 2L, "닉네임"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_DELETED_CANNOT_REPLY);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        CommunityComment comment = CommunityComment.builder().id(10L).authorId(2L).status(CommentStatus.ACTIVE).build();
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        commentService.deleteComment(10L, 2L);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인 아님")
    void deleteComment_fail_notAuthor() {
        CommunityComment comment = CommunityComment.builder().id(10L).authorId(2L).status(CommentStatus.ACTIVE).build();
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        assertThatThrownBy(() -> commentService.deleteComment(10L, 3L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_ONLY_AUTHOR_CAN_DELETE);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void deleteComment_fail_notFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.deleteComment(10L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 댓글 조회 성공")
    void getCommentsForPost_success() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).status(null).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        
        CommunityComment comment1 = CommunityComment.builder()
                .id(1L)
                .post(post)
                .parent(null)
                .authorId(2L)
                .authorNickname("유저1")
                .content("댓글1")
                .status(CommentStatus.ACTIVE)
                .build();
        
        CommunityComment comment2 = CommunityComment.builder()
                .id(2L)
                .post(post)
                .parent(null)
                .authorId(3L)
                .authorNickname("유저2")
                .content("댓글2")
                .status(CommentStatus.ACTIVE)
                .build();
        
        when(commentRepository.findAll()).thenReturn(List.of(comment1, comment2));

        // when
        List<CommunityComment> result = commentService.getCommentsForPost(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("댓글1");
        assertThat(result.get(1).getContent()).isEqualTo("댓글2");
    }

    @Test
    @DisplayName("게시글 댓글 조회 실패 - 게시글 없음")
    void getCommentsForPost_fail_postNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> commentService.getCommentsForPost(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 댓글 조회 실패 - 삭제/신고된 게시글")
    void getCommentsForPost_fail_deletedOrReportedPost() {
        CommunityPost post = CommunityPost.builder().id(1L).status(PostStatus.DELETED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        
        assertThatThrownBy(() -> commentService.getCommentsForPost(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_DELETED_OR_REPORTED);
    }
} 