package com.cheatkey.module.community.application.facade;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityPostFile;
import com.cheatkey.module.community.domain.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domain.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityPostFacadeTest {
    @Mock
    private S3FileService s3FileService;
    @Mock
    private CommunityService communityService;
    @Mock
    private FileUploadRepository fileUploadRepository;
    @Mock
    private CommunityPostFileRepository communityPostFileRepository;

    @InjectMocks
    private CommunityPostFacade communityPostFacade;

    @Test
    @DisplayName("커뮤니티 글 작성(파일 포함) 성공")
    void createPostWithFiles_success() throws Exception {
        CommunityPostCreateRequest request = mock(CommunityPostCreateRequest.class);
        when(request.getTitle()).thenReturn("테스트 제목");
        when(request.getContent()).thenReturn("테스트 내용");
        when(request.getCategory()).thenReturn(null); // 필요시 실제 enum 값으로 변경
        when(request.getUserId()).thenReturn(1L);
        when(request.getFileUploadIds()).thenReturn(List.of(100L, 101L));
        when(communityService.createPost(any(CommunityPost.class))).thenReturn(10L);
        FileUpload file1 = mock(FileUpload.class);
        FileUpload file2 = mock(FileUpload.class);
        when(fileUploadRepository.findById(100L)).thenReturn(Optional.of(file1));
        when(fileUploadRepository.findById(101L)).thenReturn(Optional.of(file2));
        when(file1.getIsTemp()).thenReturn(true);
        when(file2.getIsTemp()).thenReturn(false);
        when(file1.getS3Key()).thenReturn("uploads/temp/2024/01/15/file1.jpg");

        Long postId = communityPostFacade.createPostWithFiles(request);

        assertThat(postId).isEqualTo(10L);
        verify(communityService).createPost(any(CommunityPost.class));
        verify(fileUploadRepository, times(2)).findById(anyLong());
        verify(communityPostFileRepository, times(2)).save(any(CommunityPostFile.class));
        verify(s3FileService).moveToPermanent(eq("uploads/temp/2024/01/15/file1.jpg"), anyString());
        verify(file1).updateToPermanent(anyString());
        verify(fileUploadRepository).save(file1);
    }

    @Test
    @DisplayName("커뮤니티 글 작성 시 파일이 없는 경우")
    void createPostWithFiles_noFiles() throws Exception {
        CommunityPostCreateRequest request = mock(CommunityPostCreateRequest.class);
        when(request.getTitle()).thenReturn("테스트 제목");
        when(request.getContent()).thenReturn("테스트 내용");
        when(request.getCategory()).thenReturn(null);
        when(request.getUserId()).thenReturn(1L);
        when(request.getFileUploadIds()).thenReturn(null);
        when(communityService.createPost(any(CommunityPost.class))).thenReturn(11L);

        Long postId = communityPostFacade.createPostWithFiles(request);

        assertThat(postId).isEqualTo(11L);
        verify(communityService).createPost(any(CommunityPost.class));
        verify(fileUploadRepository, never()).findById(anyLong());
        verify(communityPostFileRepository, never()).save(any(CommunityPostFile.class));
    }

    @Test
    @DisplayName("존재하지 않는 파일 ID로 예외 발생")
    void createPostWithFiles_fileNotFound() {
        CommunityPostCreateRequest request = mock(CommunityPostCreateRequest.class);
        when(request.getTitle()).thenReturn("테스트 제목");
        when(request.getContent()).thenReturn("테스트 내용");
        when(request.getCategory()).thenReturn(null);
        when(request.getUserId()).thenReturn(1L);
        when(request.getFileUploadIds()).thenReturn(List.of(100L));
        when(communityService.createPost(any(CommunityPost.class))).thenReturn(12L);
        when(fileUploadRepository.findById(anyLong())).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(ImageException.class, () -> {
            communityPostFacade.createPostWithFiles(request);
        });
    }

    @Test
    @DisplayName("스크립트 태그가 필터링되어 저장된다")
    void createPostWithFiles_xssFiltering() throws Exception {
        CommunityPostCreateRequest request = mock(CommunityPostCreateRequest.class);
        when(request.getTitle()).thenReturn("<script>alert('x')</script>제목");
        when(request.getContent()).thenReturn("<b>굵게</b><script>alert('x')</script>");
        when(request.getCategory()).thenReturn(null); // 필요시 실제 enum 값으로 변경
        when(request.getUserId()).thenReturn(1L);
        when(request.getFileUploadIds()).thenReturn(null);
        when(communityService.createPost(any(CommunityPost.class))).thenReturn(10L);

        communityPostFacade.createPostWithFiles(request);

        org.mockito.ArgumentCaptor<CommunityPost> captor = org.mockito.ArgumentCaptor.forClass(CommunityPost.class);
        verify(communityService).createPost(captor.capture());
        CommunityPost saved = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(saved.getTitle()).doesNotContain("<script>");
        org.assertj.core.api.Assertions.assertThat(saved.getContent()).doesNotContain("<script>");
    }
} 