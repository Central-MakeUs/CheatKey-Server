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
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceBlockingSimpleTest {

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

    private CommunityPost testPost;
    private Long userId1, userId2, userId3;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        Auth user1 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_1")
                .nickname("사용자1")
                .role(AuthRole.USER)
                .build();
        user1 = authRepository.save(user1);
        userId1 = user1.getId();

        Auth user2 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_2")
                .nickname("사용자2")
                .role(AuthRole.USER)
                .build();
        user2 = authRepository.save(user2);
        userId2 = user2.getId();

        Auth user3 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao_3")
                .nickname("사용자3")
                .role(AuthRole.USER)
                .build();
        user3 = authRepository.save(user3);
        userId3 = user3.getId();

        // 게시글 생성
        testPost = CommunityPost.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(CommunityCategory.REPORT)
                .authorId(userId1)
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
                .authorId(userId2)
                .authorNickname("사용자2")
                .content("정상 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment1);

        CommunityComment comment2 = CommunityComment.builder()
                .post(testPost)
                .parent(null)
                .authorId(userId3)
                .authorNickname("사용자3")
                .content("차단될 댓글")
                .status(CommentStatus.ACTIVE)
                .build();
        commentRepository.save(comment2);

        // 사용자1이 사용자3을 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(userId1)
                .blockedId(userId3)
                .reason("차단 사유")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: 사용자1이 댓글 조회
        List<CommunityCommentResponse> result = commentService.getCommentsForPostWithBlocking(testPost.getId(), userId1);

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
}
