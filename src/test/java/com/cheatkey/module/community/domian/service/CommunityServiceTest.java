package com.cheatkey.module.community.domian.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityReportedPost;
import com.cheatkey.module.community.domian.entity.CommunityPostBlock;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityServiceTest {
    @Mock
    private CommunityPostRepository communityPostRepository;
    @Mock
    private CommunityReportedPostRepository communityReportedPostRepository;
    @Mock
    private CommunityPostBlockRepository communityPostBlockRepository;
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
} 