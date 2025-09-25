package com.cheatkey.module.community.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.service.WithdrawnUserCacheService;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceBlockingTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private WithdrawnUserCacheService withdrawnUserCacheService;

    private CommunityPost testPost;
    private Auth user1, user2, user3;

    @BeforeEach
    void setUp() {
        // 사용자 생성 (ID를 명시적으로 설정하지 않고 자동 생성)
        user1 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_1")
                .nickname("사용자1")
                .role(AuthRole.USER)
                .build();
        user1 = authRepository.save(user1);

        user2 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_2")
                .nickname("사용자2")
                .role(AuthRole.USER)
                .build();
        user2 = authRepository.save(user2);

        user3 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_3")
                .nickname("사용자3")
                .role(AuthRole.USER)
                .build();
        user3 = authRepository.save(user3);

        // 게시글 생성
        testPost = CommunityPost.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(user1.getId())
                .authorNickname("사용자1")
                .viewCount(0L)
                .status(com.cheatkey.module.community.domain.entity.PostStatus.ACTIVE)
                .build();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("차단된 사용자의 댓글이 차단 메시지로 표시됨")
    void getCommentsForPostWithBlocking_blockedUserComment() {
        // given: 댓글 생성
        CommunityComment comment1 = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user2.getId())
                .authorNickname("사용자2")
                .content("정상 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment1);

        CommunityComment comment2 = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user3.getId())
                .authorNickname("사용자3")
                .content("차단될 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment2);

        // 사용자1이 사용자3을 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 사유")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: 사용자1이 댓글 조회
        List<CommunityCommentResponse> result = commentService.getCommentsForPostWithBlocking(testPost.getId(), user1.getId());

        // then: 차단된 사용자의 댓글이 차단 메시지로 표시됨
        assertThat(result).hasSize(2);
        
        // 정상 댓글
        CommunityCommentResponse normalComment = result.stream()
                .filter(c -> c.getAuthorNickname().equals("사용자2"))
                .findFirst()
                .orElseThrow();
        assertThat(normalComment.getContent()).isEqualTo("정상 댓글");
        assertThat(normalComment.getStatus()).isEqualTo("ACTIVE");

        // 차단된 댓글
        CommunityCommentResponse blockedComment = result.stream()
                .filter(c -> c.getAuthorNickname().equals("(차단된 사용자)"))
                .findFirst()
                .orElseThrow();
        assertThat(blockedComment.getContent()).isEqualTo("차단한 사용자의 댓글입니다.");
        assertThat(blockedComment.getStatus()).isEqualTo("BLOCKED_BY_USER");
    }

    @Test
    @DisplayName("차단된 사용자의 대댓글이 차단 메시지로 표시됨")
    void getCommentsForPostWithBlocking_blockedUserReply() {
        // given: 댓글과 대댓글 생성
        CommunityComment parentComment = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user2.getId())
                .authorNickname("사용자2")
                .content("부모 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        parentComment = commentRepository.save(parentComment);

        CommunityComment reply1 = CommunityComment.builder()
                .post(testPost)
                .parent(parentComment)
                .authorId(user2.getId())
                .authorNickname("사용자2")
                .content("정상 대댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(reply1);

        CommunityComment reply2 = CommunityComment.builder()
                .post(testPost)
                .parent(parentComment)
                .authorId(user3.getId())
                .authorNickname("사용자3")
                .content("차단될 대댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(reply2);

        // 사용자1이 사용자3을 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 사유")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: 사용자1이 댓글 조회
        List<CommunityCommentResponse> result = commentService.getCommentsForPostWithBlocking(testPost.getId(), user1.getId());

        // then: 차단된 사용자의 대댓글이 차단 메시지로 표시됨
        assertThat(result).hasSize(1);
        
        CommunityCommentResponse parent = result.get(0);
        assertThat(parent.getContent()).isEqualTo("부모 댓글");
        assertThat(parent.getChildren()).hasSize(2);

        // 정상 대댓글
        CommunityCommentResponse normalReply = parent.getChildren().stream()
                .filter(c -> c.getAuthorNickname().equals("사용자2"))
                .findFirst()
                .orElseThrow();
        assertThat(normalReply.getContent()).isEqualTo("정상 대댓글");
        assertThat(normalReply.getStatus()).isEqualTo("ACTIVE");

        // 차단된 대댓글
        CommunityCommentResponse blockedReply = parent.getChildren().stream()
                .filter(c -> c.getAuthorNickname().equals("(차단된 사용자)"))
                .findFirst()
                .orElseThrow();
        assertThat(blockedReply.getContent()).isEqualTo("차단한 사용자의 댓글입니다.");
        assertThat(blockedReply.getStatus()).isEqualTo("BLOCKED_BY_USER");
    }

    @Test
    @DisplayName("부모 댓글이 차단되면 자식 댓글들이 미노출됨")
    void getCommentsForPostWithBlocking_blockedParentCommentHidesChildren() {
        // given: 부모 댓글과 자식 댓글들 생성
        CommunityComment parentComment = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user2.getId())
                .authorNickname("사용자2")
                .content("차단될 부모 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        parentComment = commentRepository.save(parentComment);

        // 자식 댓글 1
        CommunityComment child1 = CommunityComment.builder()
                .post(testPost)
                .parent(parentComment)
                .authorId(user1.getId())
                .authorNickname("사용자1")
                .content("자식 댓글 1")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(child1);

        // 자식 댓글 2
        CommunityComment child2 = CommunityComment.builder()
                .post(testPost)
                .parent(parentComment)
                .authorId(user3.getId())
                .authorNickname("사용자3")
                .content("자식 댓글 2")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(child2);

        // 사용자1이 사용자2(부모 댓글 작성자)를 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .reason("차단 사유")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: 사용자1이 댓글 조회
        List<CommunityCommentResponse> result = commentService.getCommentsForPostWithBlocking(testPost.getId(), user1.getId());

        // then: 부모 댓글은 차단 메시지로 표시되고, 자식 댓글들은 미노출됨
        assertThat(result).hasSize(1);
        
        CommunityCommentResponse blockedParent = result.get(0);
        assertThat(blockedParent.getAuthorNickname()).isEqualTo("(차단된 사용자)");
        assertThat(blockedParent.getContent()).isEqualTo("차단한 사용자의 댓글입니다.");
        assertThat(blockedParent.getStatus()).isEqualTo("BLOCKED_BY_USER");
        assertThat(blockedParent.getChildren()).isEmpty(); // ← 자식 댓글들 미노출!
    }

    @Test
    @DisplayName("차단되지 않은 사용자에게는 정상적으로 댓글이 표시됨")
    void getCommentsForPostWithBlocking_normalUserSeesAllComments() {
        // given: 댓글 생성
        CommunityComment comment1 = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user2.getId())
                .authorNickname("사용자2")
                .content("댓글1")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment1);

        CommunityComment comment2 = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(user3.getId())
                .authorNickname("사용자3")
                .content("댓글2")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment2);

        // 사용자1이 사용자3을 차단 (하지만 사용자2는 차단하지 않음)
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 사유")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: 사용자2가 댓글 조회 (차단하지 않은 사용자)
        List<CommunityCommentResponse> result = commentService.getCommentsForPostWithBlocking(testPost.getId(), user2.getId());

        // then: 모든 댓글이 정상적으로 표시됨
        assertThat(result).hasSize(2);
        
        CommunityCommentResponse comment1Response = result.stream()
                .filter(c -> c.getAuthorNickname().equals("사용자2"))
                .findFirst()
                .orElseThrow();
        assertThat(comment1Response.getContent()).isEqualTo("댓글1");
        assertThat(comment1Response.getStatus()).isEqualTo("ACTIVE");

        CommunityCommentResponse comment2Response = result.stream()
                .filter(c -> c.getAuthorNickname().equals("사용자3"))
                .findFirst()
                .orElseThrow();
        assertThat(comment2Response.getContent()).isEqualTo("댓글2");
        assertThat(comment2Response.getStatus()).isEqualTo("ACTIVE");
    }
}
