package com.cheatkey.module.home;

import com.cheatkey.common.jwt.JwtProvider;
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
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 홈 화면 대시보드의 차단 기능 통합 테스트
 * 차단한 사용자의 게시글이 인기글 목록에서 제외되는지 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeDashboardBlockingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityCommentRepository communityCommentRepository;

    @Autowired
    private CommunityPostBlockRepository communityPostBlockRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Auth currentUser;
    private Auth blockedUser;
    private Auth normalUser;
    private CommunityPost blockedUserPost;
    private CommunityPost normalUserPost;
    private String currentUserToken;

    @BeforeEach
    void setUp() {
        // 현재 사용자 생성
        currentUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("current-user-" + System.currentTimeMillis())
                .email("current@test.com")
                .nickname("현재사용자")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        currentUser = authRepository.save(currentUser);
        currentUserToken = jwtProvider.createAccessToken(currentUser.getId(), Provider.KAKAO, AuthRole.USER);

        // 차단될 사용자 생성
        blockedUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("blocked-user-" + System.currentTimeMillis())
                .email("blocked@test.com")
                .nickname("차단될사용자")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        blockedUser = authRepository.save(blockedUser);

        // 정상 사용자 생성
        normalUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("normal-user-" + System.currentTimeMillis())
                .email("normal@test.com")
                .nickname("정상사용자")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        normalUser = authRepository.save(normalUser);

        // 차단될 사용자의 인기 게시글 생성 (댓글 5개)
        blockedUserPost = CommunityPost.builder()
                .title("차단될 사용자의 인기글")
                .content("이 글은 차단된 사용자의 글입니다.")
                .category(CommunityCategory.REPORT)
                .authorId(blockedUser.getId())
                .authorNickname(blockedUser.getNickname())
                .viewCount(100L)
                .status(PostStatus.ACTIVE)
                .build();
        blockedUserPost = communityPostRepository.save(blockedUserPost);

        // 차단될 사용자의 게시글에 댓글 5개 추가
        for (int i = 1; i <= 5; i++) {
            CommunityComment comment = CommunityComment.builder()
                    .post(blockedUserPost)
                    .authorId(normalUser.getId())
                    .authorNickname(normalUser.getNickname())
                    .content("댓글 " + i)
                    .status(CommentStatus.ACTIVE)
                    .build();
            communityCommentRepository.save(comment);
        }

        // 정상 사용자의 인기 게시글 생성 (댓글 3개)
        normalUserPost = CommunityPost.builder()
                .title("정상 사용자의 인기글")
                .content("이 글은 정상 사용자의 글입니다.")
                .category(CommunityCategory.REPORT)
                .authorId(normalUser.getId())
                .authorNickname(normalUser.getNickname())
                .viewCount(50L)
                .status(PostStatus.ACTIVE)
                .build();
        normalUserPost = communityPostRepository.save(normalUserPost);

        // 정상 사용자의 게시글에 댓글 3개 추가
        for (int i = 1; i <= 3; i++) {
            CommunityComment comment = CommunityComment.builder()
                    .post(normalUserPost)
                    .authorId(currentUser.getId())
                    .authorNickname(currentUser.getNickname())
                    .content("댓글 " + i)
                    .status(CommentStatus.ACTIVE)
                    .build();
            communityCommentRepository.save(comment);
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        communityCommentRepository.deleteAll();
        communityPostRepository.deleteAll();
        communityPostBlockRepository.deleteAll();
        authRepository.deleteAll();
    }

    @Test
    @DisplayName("차단하지 않은 상태에서는 모든 인기글이 표시됨")
    void getDashboard_withoutBlocking_showsAllPopularPosts() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + currentUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularPosts", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.popularPosts[*].title", hasItem("차단될 사용자의 인기글")))
                .andExpect(jsonPath("$.popularPosts[*].title", hasItem("정상 사용자의 인기글")))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", hasItem("차단될사용자")))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", hasItem("정상사용자")));
    }

    @Test
    @DisplayName("사용자를 차단하면 해당 사용자의 게시글이 홈 인기글 목록에서 제외됨")
    void getDashboard_withBlocking_hidesBlockedUserPosts() throws Exception {
        // given: 현재 사용자가 차단될 사용자를 차단
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(currentUser.getId())
                .blockedId(blockedUser.getId())
                .reason("테스트 차단")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);

        // when & then
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + currentUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularPosts").isArray())
                // 차단된 사용자의 게시글은 표시되지 않음
                .andExpect(jsonPath("$.popularPosts[*].title", not(hasItem("차단될 사용자의 인기글"))))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", not(hasItem("차단될사용자"))))
                // 정상 사용자의 게시글은 표시됨
                .andExpect(jsonPath("$.popularPosts[*].title", hasItem("정상 사용자의 인기글")))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", hasItem("정상사용자")));
    }

    @Test
    @DisplayName("차단이 비활성화되면 다시 해당 사용자의 게시글이 표시됨")
    void getDashboard_withInactiveBlocking_showsBlockedUserPosts() throws Exception {
        // given: 비활성화된 차단 설정
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(currentUser.getId())
                .blockedId(blockedUser.getId())
                .reason("테스트 차단")
                .isActive(false)  // 비활성화
                .build();
        communityPostBlockRepository.save(block);

        // when & then: 비활성화된 차단은 적용되지 않음
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + currentUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularPosts", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.popularPosts[*].title", hasItem("차단될 사용자의 인기글")))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", hasItem("차단될사용자")));
    }

    @Test
    @DisplayName("여러 사용자를 차단하면 모두 인기글 목록에서 제외됨")
    void getDashboard_withMultipleBlockings_hidesAllBlockedUsersPosts() throws Exception {
        // given: 또 다른 사용자와 게시글 생성
        Auth anotherBlockedUser = Auth.builder()
                .provider(Provider.KAKAO)
                .providerId("another-blocked-" + System.currentTimeMillis())
                .email("another@test.com")
                .nickname("또다른차단될사용자")
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();
        anotherBlockedUser = authRepository.save(anotherBlockedUser);

        CommunityPost anotherBlockedPost = CommunityPost.builder()
                .title("또다른 차단될 사용자의 글")
                .content("이것도 차단될 글입니다.")
                .category(CommunityCategory.REPORT)
                .authorId(anotherBlockedUser.getId())
                .authorNickname(anotherBlockedUser.getNickname())
                .viewCount(80L)
                .status(PostStatus.ACTIVE)
                .build();
        communityPostRepository.save(anotherBlockedPost);

        // 두 사용자 모두 차단
        CommunityPostBlock block1 = CommunityPostBlock.builder()
                .blockerId(currentUser.getId())
                .blockedId(blockedUser.getId())
                .reason("테스트 차단 1")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block1);

        CommunityPostBlock block2 = CommunityPostBlock.builder()
                .blockerId(currentUser.getId())
                .blockedId(anotherBlockedUser.getId())
                .reason("테스트 차단 2")
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block2);

        // when & then
        mockMvc.perform(get("/v1/api/home/dashboard")
                        .header("Authorization", "Bearer " + currentUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularPosts").isArray())
                // 차단된 두 사용자의 게시글 모두 표시되지 않음
                .andExpect(jsonPath("$.popularPosts[*].title", not(hasItem("차단될 사용자의 인기글"))))
                .andExpect(jsonPath("$.popularPosts[*].title", not(hasItem("또다른 차단될 사용자의 글"))))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", not(hasItem("차단될사용자"))))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", not(hasItem("또다른차단될사용자"))))
                // 정상 사용자의 게시글은 표시됨
                .andExpect(jsonPath("$.popularPosts[*].title", hasItem("정상 사용자의 인기글")))
                .andExpect(jsonPath("$.popularPosts[*].authorNickname", hasItem("정상사용자")));
    }
}

