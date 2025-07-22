package com.cheatkey.module.community.domian.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityPostBlock;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domian.repository.CommunityCommentRepository;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import com.cheatkey.module.community.domian.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.community.domian.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;

class CommunityPostDetailServiceTest {
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
    @Mock
    private CommentService commentService;
    @InjectMocks
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void 정상_게시글_상세_조회_및_조회수_증가_정책_테스트() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.ACTIVE).viewCount(0L).build();
        when(communityPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(communityPostBlockRepository.findAll()).thenReturn(Collections.emptyList());
        when(communityPostFileRepository.findAll()).thenReturn(Collections.emptyList());
        when(fileUploadRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(commentService.getCommentsForPost(1L)).thenReturn(Collections.emptyList());
        when(communityPostMapper.toDetailDto(any(), anyInt(), anyList(), anyList(), anyBoolean(), anyBoolean(), any())).thenReturn(
            CommunityPostDetailResponse.builder().id(1L).authorNickname("닉네임").commentCount(0).build()
        );

        // when
        CommunityPostDetailResponse result = communityService.getPostDetail(3L, 1L);

        // then
        assertEquals("닉네임", result.getAuthorNickname());
        assertEquals(0, result.getCommentCount());
        assertEquals(1L, post.getViewCount()); // viewCount 증가 확인
        // viewCount 증가 시에는 save가 호출되어야 함
        verify(communityPostRepository, atLeastOnce()).save(any());
    }

    @Test
    void 본인글_삭제상태_상세조회_예외_정책_테스트() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.DELETED).build();
        when(communityPostRepository.findById(1L)).thenReturn(Optional.of(post));

        // when & then
        CustomException ex = assertThrows(CustomException.class, () -> communityService.getPostDetail(2L, 1L));
        assertEquals(ErrorCode.POST_NOT_FOUND, ex.getErrorCode());
        // 예외 발생 시 save가 호출되지 않아야 함
        verify(communityPostRepository, never()).save(any());
    }

    @Test
    void 본인글_신고상태_상세조회_차단메시지_정책_테스트() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.REPORTED).build();
        when(communityPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(communityPostBlockRepository.findAll()).thenReturn(Collections.emptyList());
        when(communityPostMapper.toDetailDto(any(), anyInt(), anyList(), anyList(), anyBoolean(), anyBoolean(), any())).thenReturn(
            CommunityPostDetailResponse.builder().id(1L).authorNickname("닉네임").commentCount(0).blockMessage("차단된 글입니다.").build()
        );

        // when
        CommunityPostDetailResponse result = communityService.getPostDetail(2L, 1L);

        // then
        assertEquals("차단된 글입니다.", result.getBlockMessage());
        verify(communityPostRepository, never()).save(any());
    }

    @Test
    void 비정상상태_타인글_상세조회_차단메시지_정책_테스트() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.REPORTED).build();
        when(communityPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(communityPostBlockRepository.findAll()).thenReturn(Collections.emptyList());
        when(communityPostMapper.toDetailDto(any(), anyInt(), anyList(), anyList(), anyBoolean(), anyBoolean(), any())).thenReturn(
            CommunityPostDetailResponse.builder().id(1L).authorNickname("닉네임").commentCount(0).blockMessage("차단된 글입니다.").build()
        );

        // when
        CommunityPostDetailResponse result = communityService.getPostDetail(3L, 1L);

        // then
        assertEquals("차단된 글입니다.", result.getBlockMessage());
        verify(communityPostRepository, never()).save(any());
    }

    @Test
    void 차단유저_글_상세조회_차단메시지_정책_테스트() {
        // given
        CommunityPost post = CommunityPost.builder().id(1L).userId(2L).nickname("닉네임").status(PostStatus.ACTIVE).build();
        when(communityPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(communityPostBlockRepository.findAll()).thenReturn(List.of(
            CommunityPostBlock.builder().blockerId(3L).blockedId(2L).isActive(true).build()
        ));
        when(communityPostMapper.toDetailDto(any(), anyInt(), anyList(), anyList(), anyBoolean(), anyBoolean(), any())).thenReturn(
            CommunityPostDetailResponse.builder().id(1L).authorNickname("닉네임").commentCount(0).blockMessage("차단된 글입니다.").build()
        );

        // when
        CommunityPostDetailResponse result = communityService.getPostDetail(3L, 1L);

        // then
        assertEquals("차단된 글입니다.", result.getBlockMessage());
        verify(communityPostRepository, never()).save(any());
    }
} 