package com.cheatkey.module.community.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityReportedPost;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domain.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;

class CommunityServiceTest {
    @Mock
    private CommunityPostRepository communityPostRepository;
    @Mock
    private CommunityReportedPostRepository communityReportedPostRepository;
    @Mock
    private CommunityPostBlockRepository communityPostBlockRepository;
    @Mock
    private CommunityCommentRepository communityCommentRepository;
    @Mock
    private CommunityPostFileRepository communityPostFileRepository;
    @Mock
    private FileUploadRepository fileUploadRepository;
    @Mock
    private FileService fileService;
    @Mock
    private CommentService commentService;
    @Mock
    private CommunityPostMapper communityPostMapper;
    @InjectMocks
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void reportPost_success() {
        // given
        Long postId = 1L;
        Long reporterId = 2L;
        String reasonCode = "FAKE";
        CommunityPost post = CommunityPost.builder()
                .id(postId)
                .authorId(3L)
                .status(PostStatus.ACTIVE)
                .build();
        when(communityReportedPostRepository.existsByPostIdAndReporterId(postId, reporterId)).thenReturn(false);
        when(communityPostRepository.findById(postId)).thenReturn(Optional.of(post));

        // when & then
        assertDoesNotThrow(() -> communityService.reportPost(postId, reporterId, reasonCode));

        // then
        verify(communityReportedPostRepository).save(any(CommunityReportedPost.class));
        verify(communityPostRepository).save(any(CommunityPost.class));
    }

    @Test
    void reportPost_alreadyReported() {
        // given
        Long postId = 1L;
        Long reporterId = 2L;
        when(communityReportedPostRepository.existsByPostIdAndReporterId(postId, reporterId)).thenReturn(true);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> communityService.reportPost(postId, reporterId, "FAKE"));

        // then
        assertEquals(ErrorCode.POST_ALREADY_REPORTED, ex.getErrorCode());
    }

    @Test
    void reportPost_postNotFound() {
        // given
        Long postId = 1L;
        Long reporterId = 2L;
        when(communityReportedPostRepository.existsByPostIdAndReporterId(postId, reporterId)).thenReturn(false);
        when(communityPostRepository.findById(postId)).thenReturn(Optional.empty());

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> communityService.reportPost(postId, reporterId, "FAKE"));

        // then
        assertEquals(ErrorCode.POST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void blockUser_success() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;
        String reason = "HATE";
        when(communityPostBlockRepository.existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId)).thenReturn(false);

        // when & then
        assertDoesNotThrow(() -> communityService.blockUser(blockerId, blockedId, reason));

        // then
        verify(communityPostBlockRepository).save(any(CommunityPostBlock.class));
    }

    @Test
    void blockUser_alreadyBlocked() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;
        when(communityPostBlockRepository.existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId)).thenReturn(true);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> communityService.blockUser(blockerId, blockedId, "HATE"));

        // then
        assertEquals(ErrorCode.USER_ALREADY_BLOCKED, ex.getErrorCode());
    }

    @Test
    void getPostDetail_success() {
        // given
        Long userId = 1L;
        Long postId = 1L;
        CommunityPost post = CommunityPost.builder()
                .id(postId)
                .authorId(3L)
                .authorNickname("테스트유저")
                .status(PostStatus.ACTIVE)
                .viewCount(0L)
                .build();
        
        List<CommunityComment> comments = List.of(
            CommunityComment.builder().id(1L).content("댓글1").build(),
            CommunityComment.builder().id(2L).content("댓글2").build()
        );
        
        List<CommunityCommentResponse> commentResponses = List.of(
            CommunityCommentResponse.builder().id(1L).content("댓글1").build(),
            CommunityCommentResponse.builder().id(2L).content("댓글2").build()
        );
        
        CommunityPostDetailResponse expectedResponse = CommunityPostDetailResponse.builder()
                .id(postId)
                .commentCount(2)
                .comments(commentResponses)
                .build();
        
        when(communityPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(communityPostBlockRepository.findAll()).thenReturn(List.of());
        when(communityPostFileRepository.findAll()).thenReturn(List.of());
        when(commentService.getCommentsForPost(postId)).thenReturn(comments);
        when(communityPostMapper.toCommentDtoList(comments)).thenReturn(commentResponses);
        when(communityPostMapper.toDetailDto(eq(post), eq(2), anyList(), eq(commentResponses), eq(true), eq(false), isNull()))
                .thenReturn(expectedResponse);

        // when
        CommunityPostDetailResponse result = communityService.getPostDetail(userId, postId);

        // then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(commentService).getCommentsForPost(postId);
        verify(communityPostMapper).toCommentDtoList(comments);
    }

    @Test
    void getPopularPosts_댓글수_기준_정렬() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = List.of(
            CommunityPost.builder().id(1L).title("글1").viewCount(10L).createdAt(now.minusDays(1)).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(2L).title("글2").viewCount(20L).createdAt(now).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(3L).title("글3").viewCount(5L).createdAt(now.minusHours(1)).status(PostStatus.ACTIVE).build()
        );
        
        // 댓글 수: 글2(5개) > 글1(3개) > 글3(0개)
        List<Object[]> commentCounts = List.of(
            new Object[]{1L, 3L},
            new Object[]{2L, 5L}
        );
        
        when(communityPostRepository.findAll()).thenReturn(posts);
        when(communityCommentRepository.countCommentsByPostIds(List.of(1L, 2L, 3L))).thenReturn(commentCounts);

        // when
        List<CommunityPost> result = communityService.getPopularPosts(3);

        // then
        assertEquals(3, result.size());
        // 댓글 수 기준 정렬: 글2(5개) > 글1(3개) > 글3(0개)
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    void getPopularPosts_동일한_댓글수_조회수_기준_정렬() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = List.of(
            CommunityPost.builder().id(1L).title("글1").viewCount(10L).createdAt(now.minusDays(1)).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(2L).title("글2").viewCount(20L).createdAt(now).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(3L).title("글3").viewCount(5L).createdAt(now.minusHours(1)).status(PostStatus.ACTIVE).build()
        );
        
        // 댓글 수가 모두 동일 (3개씩)
        List<Object[]> commentCounts = List.of(
            new Object[]{1L, 3L},
            new Object[]{2L, 3L},
            new Object[]{3L, 3L}
        );
        
        when(communityPostRepository.findAll()).thenReturn(posts);
        when(communityCommentRepository.countCommentsByPostIds(List.of(1L, 2L, 3L))).thenReturn(commentCounts);

        // when
        List<CommunityPost> result = communityService.getPopularPosts(3);

        // then
        assertEquals(3, result.size());
        // 댓글 수 동일하므로 조회수 기준 정렬: 글2(20) > 글1(10) > 글3(5)
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    void getPopularPosts_동일한_댓글수_조회수_작성일시_기준_정렬() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = List.of(
            CommunityPost.builder().id(1L).title("글1").viewCount(10L).createdAt(now.minusDays(1)).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(2L).title("글2").viewCount(10L).createdAt(now).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(3L).title("글3").viewCount(10L).createdAt(now.minusHours(1)).status(PostStatus.ACTIVE).build()
        );
        
        // 댓글 수와 조회수가 모두 동일
        List<Object[]> commentCounts = List.of(
            new Object[]{1L, 3L},
            new Object[]{2L, 3L},
            new Object[]{3L, 3L}
        );
        
        when(communityPostRepository.findAll()).thenReturn(posts);
        when(communityCommentRepository.countCommentsByPostIds(List.of(1L, 2L, 3L))).thenReturn(commentCounts);

        // when
        List<CommunityPost> result = communityService.getPopularPosts(3);

        // then
        assertEquals(3, result.size());
        // 댓글 수, 조회수 동일하므로 작성일시 기준 정렬: 글2(최신) > 글3 > 글1(오래됨)
        assertEquals(2L, result.get(0).getId());
        assertEquals(3L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());
    }

    @Test
    void getPopularPosts_댓글없는_게시글_조회수_기준_정렬() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = List.of(
            CommunityPost.builder().id(1L).title("글1").viewCount(10L).createdAt(now.minusDays(1)).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(2L).title("글2").viewCount(20L).createdAt(now).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(3L).title("글3").viewCount(5L).createdAt(now.minusHours(1)).status(PostStatus.ACTIVE).build()
        );
        
        // 댓글이 없는 게시글들
        List<Object[]> commentCounts = List.of();
        
        when(communityPostRepository.findAll()).thenReturn(posts);
        when(communityCommentRepository.countCommentsByPostIds(List.of(1L, 2L, 3L))).thenReturn(commentCounts);

        // when
        List<CommunityPost> result = communityService.getPopularPosts(3);

        // then
        assertEquals(3, result.size());
        // 댓글 수가 모두 0이므로 조회수 기준 정렬: 글2(20) > 글1(10) > 글3(5)
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    void getPopularPosts_ACTIVE_상태만_필터링() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = List.of(
            CommunityPost.builder().id(1L).title("글1").viewCount(10L).createdAt(now).status(PostStatus.ACTIVE).build(),
            CommunityPost.builder().id(2L).title("글2").viewCount(20L).createdAt(now).status(PostStatus.DELETED).build(),
            CommunityPost.builder().id(3L).title("글3").viewCount(5L).createdAt(now).status(PostStatus.ACTIVE).build()
        );
        
        List<Object[]> commentCounts = List.of(
            new Object[]{1L, 3L},
            new Object[]{3L, 1L}
        );
        
        when(communityPostRepository.findAll()).thenReturn(posts);
        when(communityCommentRepository.countCommentsByPostIds(List.of(1L, 3L))).thenReturn(commentCounts);

        // when
        List<CommunityPost> result = communityService.getPopularPosts(3);

        // then
        assertEquals(2, result.size());
        // ACTIVE 상태만 필터링되어 글1, 글3만 포함
        assertTrue(result.stream().allMatch(post -> post.getStatus() == PostStatus.ACTIVE));
        // 댓글 수 기준 정렬: 글1(3개) > 글3(1개)
        assertEquals(1L, result.get(0).getId());
        assertEquals(3L, result.get(1).getId());
    }
} 