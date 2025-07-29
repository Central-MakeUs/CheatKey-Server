package com.cheatkey.module.community.domian.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityReportedPost;
import com.cheatkey.module.community.domian.entity.CommunityPostBlock;
import com.cheatkey.module.community.domian.entity.CommunityPostFile;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.entity.comment.CommunityComment;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domian.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domian.service.CommentService;
import com.cheatkey.module.community.domian.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import com.cheatkey.module.file.domain.entity.FileUpload;
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
                .userId(3L)
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
                .userId(userId)
                .nickname("테스트유저")
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
} 