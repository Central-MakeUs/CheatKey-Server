package com.cheatkey.module.community.domian.service;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityPostBlock;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.community.domian.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Transactional
class CommunityPostListServiceTest {
    @Mock
    private CommunityPostRepository communityPostRepository;
    @Mock
    private CommunityPostBlockRepository communityPostBlockRepository;
    @Mock
    private CommunityPostFileRepository communityPostFileRepository;
    @Mock
    private CommunityCommentRepository communityCommentRepository;
    @Mock
    private FileUploadRepository fileUploadRepository;
    @Mock
    private FileService fileService;
    @Mock
    private CommunityPostMapper communityPostMapper;
    @InjectMocks
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void 정상_상태_및_차단_제외_댓글수_닉네임_정책_테스트() {
        // given
        CommunityPost activePost = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.ACTIVE).build();
        CommunityPost deletedPost = CommunityPost.builder().id(2L).userId(3L).nickname("닉네임2").status(PostStatus.DELETED).build();
        CommunityPost blockedPost = CommunityPost.builder().id(3L).userId(4L).nickname("닉네임3").status(PostStatus.ACTIVE).build();

        List<CommunityPost> allPosts = List.of(activePost, deletedPost, blockedPost);
        when(communityPostRepository.findAllByCustomConditions(any(), any(), any(), any())).thenReturn(new PageImpl<>(allPosts));
        when(communityPostBlockRepository.findAll()).thenReturn(List.of(
            CommunityPostBlock.builder().blockerId(1L).blockedId(4L).isActive(true).build()
        ));

        List<Object[]> commentCounts = new java.util.ArrayList<>();
        commentCounts.add(new Object[]{1L, 2L});
        when(communityCommentRepository.countCommentsByPostIds(any())).thenReturn(commentCounts);
        when(communityPostFileRepository.findAll()).thenReturn(Collections.emptyList());
        when(communityPostMapper.toListDto(any(), anyInt(), anyList())).thenReturn(
            CommunityPostListResponse.builder().id(1L).authorNickname("닉네임").commentCount(2).thumbnailUrls(List.of()).build()
        );

        // when
        Page<CommunityPostListResponse> result = communityService.getPostList(1L, null, "latest", PageRequest.of(0, 20));

        // then
        assertEquals(1, result.getContent().size());
        assertEquals("닉네임", result.getContent().get(0).getAuthorNickname());
        assertEquals(2, result.getContent().get(0).getCommentCount());
        verify(communityPostRepository, never()).save(any());
    }
} 