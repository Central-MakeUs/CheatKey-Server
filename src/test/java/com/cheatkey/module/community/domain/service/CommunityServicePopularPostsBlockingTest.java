package com.cheatkey.module.community.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommunityService의 getPopularPostsWithAuthorInfo 메서드의 차단 기능 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommunityServicePopularPostsBlockingTest {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityCommentRepository communityCommentRepository;

    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    @Autowired
    private AuthRepository authRepository;

    private Auth user1, user2, user3;
    private CommunityPost post1, post2, post3;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        user1 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("user1_" + System.currentTimeMillis())
                .nickname("사용자1")
                .email("user1@test.com")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        user1 = authRepository.save(user1);

        user2 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("user2_" + System.currentTimeMillis())
                .nickname("사용자2")
                .email("user2@test.com")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        user2 = authRepository.save(user2);

        user3 = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("user3_" + System.currentTimeMillis())
                .nickname("사용자3")
                .email("user3@test.com")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        user3 = authRepository.save(user3);

        // 게시글 생성 (댓글 수를 다르게 하여 인기도 순위 설정)
        
        // post1: 사용자1 작성, 댓글 5개
        post1 = CommunityPost.builder()
                .title("게시글1")
                .content("내용1")
                .category(CommunityCategory.REPORT)
                .authorId(user1.getId())
                .authorNickname(user1.getNickname())
                .viewCount(10L)
                .status(PostStatus.ACTIVE)
                .build();
        post1 = communityPostRepository.save(post1);
        createComments(post1, 5);

        // post2: 사용자2 작성, 댓글 3개
        post2 = CommunityPost.builder()
                .title("게시글2")
                .content("내용2")
                .category(CommunityCategory.REPORT)
                .authorId(user2.getId())
                .authorNickname(user2.getNickname())
                .viewCount(20L)
                .status(PostStatus.ACTIVE)
                .build();
        post2 = communityPostRepository.save(post2);
        createComments(post2, 3);

        // post3: 사용자3 작성, 댓글 1개
        post3 = CommunityPost.builder()
                .title("게시글3")
                .content("내용3")
                .category(CommunityCategory.REPORT)
                .authorId(user3.getId())
                .authorNickname(user3.getNickname())
                .viewCount(30L)
                .status(PostStatus.ACTIVE)
                .build();
        post3 = communityPostRepository.save(post3);
        createComments(post3, 1);
    }

    private void createComments(CommunityPost post, int count) {
        for (int i = 0; i < count; i++) {
            CommunityComment comment = CommunityComment.builder()
                    .post(post)
                    .authorId(user1.getId())
                    .authorNickname(user1.getNickname())
                    .content("댓글 " + i)
                    .status(CommentStatus.ACTIVE)
                    .build();
            communityCommentRepository.save(comment);
        }
    }

    @Test
    @DisplayName("차단하지 않은 경우 모든 인기글이 댓글 수 순으로 조회됨")
    void getPopularPostsWithAuthorInfo_withoutBlocking_returnsAllPosts() {
        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(3);
        // 댓글 수 기준 정렬: post1(5개) > post2(3개) > post3(1개)
        assertThat(result.get(0).getPost().getId()).isEqualTo(post1.getId());
        assertThat(result.get(1).getPost().getId()).isEqualTo(post2.getId());
        assertThat(result.get(2).getPost().getId()).isEqualTo(post3.getId());
    }

    @Test
    @DisplayName("사용자를 차단하면 해당 사용자의 게시글이 제외됨")
    void getPopularPostsWithAuthorInfo_withBlocking_excludesBlockedUserPosts() {
        // given: user1이 user2를 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .reason("차단 테스트")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(2);
        // post2(user2의 글)는 제외됨
        assertThat(result).noneMatch(p -> p.getPost().getId().equals(post2.getId()));
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post1.getId()));
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post3.getId()));
    }

    @Test
    @DisplayName("여러 사용자를 차단하면 모두 제외됨")
    void getPopularPostsWithAuthorInfo_withMultipleBlockings_excludesAllBlockedUsersPosts() {
        // given: user1이 user2와 user3를 차단
        CommunityPostBlock block1 = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .reason("차단 테스트 1")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block1);

        CommunityPostBlock block2 = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 테스트 2")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block2);

        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(1);
        // post1만 남음 (user1 자신의 글)
        assertThat(result.get(0).getPost().getId()).isEqualTo(post1.getId());
        assertThat(result).noneMatch(p -> p.getPost().getId().equals(post2.getId()));
        assertThat(result).noneMatch(p -> p.getPost().getId().equals(post3.getId()));
    }

    @Test
    @DisplayName("비활성화된 차단은 필터링에 적용되지 않음")
    void getPopularPostsWithAuthorInfo_withInactiveBlocking_includesBlockedUserPosts() {
        // given: user1이 user2를 차단했지만 비활성화됨
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .reason("차단 테스트")
                .isActive(false)  // 비활성화
                .build();
        communityPostBlockRepository.save(block);

        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(3);
        // 비활성화된 차단은 적용되지 않으므로 post2도 포함됨
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post2.getId()));
    }

    @Test
    @DisplayName("차단 후에도 정렬 순서는 댓글 수 기준으로 유지됨")
    void getPopularPostsWithAuthorInfo_withBlocking_maintainsSortingOrder() {
        // given: user1이 user3를 차단 (댓글 수가 가장 적은 게시글 작성자)
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 테스트")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(2);
        // 댓글 수 기준 정렬 유지: post1(5개) > post2(3개)
        assertThat(result.get(0).getPost().getId()).isEqualTo(post1.getId());
        assertThat(result.get(1).getPost().getId()).isEqualTo(post2.getId());
    }

    @Test
    @DisplayName("limit이 적용되고 차단 필터링도 함께 적용됨")
    void getPopularPostsWithAuthorInfo_withBlockingAndLimit_returnsCorrectResults() {
        // given: user1이 user3를 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .reason("차단 테스트")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: limit=1로 조회
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 1);

        // then
        assertThat(result).hasSize(1);
        // 차단 필터링 후 댓글 수가 가장 많은 post1만 반환
        assertThat(result.get(0).getPost().getId()).isEqualTo(post1.getId());
    }

    @Test
    @DisplayName("작성자 정보가 함께 조회됨")
    void getPopularPostsWithAuthorInfo_includesAuthorInfo() {
        // when
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user1.getId(), 10);

        // then
        assertThat(result).hasSize(3);
        result.forEach(postWithAuthorInfo -> {
            assertThat(postWithAuthorInfo.getPost()).isNotNull();
            assertThat(postWithAuthorInfo.getAuthor()).isNotNull();
            assertThat(postWithAuthorInfo.getAuthor().getId())
                    .isEqualTo(postWithAuthorInfo.getPost().getAuthorId());
        });
    }

    @Test
    @DisplayName("다른 사용자는 차단 영향을 받지 않음")
    void getPopularPostsWithAuthorInfo_differentUser_notAffectedByBlocking() {
        // given: user1이 user2를 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .reason("차단 테스트")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when: user3가 인기글 조회
        List<CommunityService.CommunityPostWithAuthorInfo> result = 
                communityService.getPopularPostsWithAuthorInfo(user3.getId(), 10);

        // then: user3는 차단하지 않았으므로 모든 글 조회 가능
        assertThat(result).hasSize(3);
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post1.getId()));
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post2.getId()));
        assertThat(result).anyMatch(p -> p.getPost().getId().equals(post3.getId()));
    }
}

