package com.cheatkey.module.community.application.facade;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityPostFile;
import com.cheatkey.module.community.domian.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;

@Service
@RequiredArgsConstructor
public class CommunityPostFacade {

    private final S3FileService s3FileService;
    private final CommunityService communityService;
    private final FileUploadRepository fileUploadRepository;

    private final CommunityPostFileRepository communityPostFileRepository;

    @Transactional
    public Long createPostWithFiles(CommunityPostCreateRequest request) throws ImageException {
        // 1. 커뮤니티 글 등록
        String safeTitle = Jsoup.clean(request.getTitle(), Safelist.none());
        String safeContent = Jsoup.clean(request.getContent(), Safelist.basic());
        CommunityPost post = CommunityPost.createPost(
            safeTitle,
            safeContent,
            request.getCategory(),
            request.getUserId()
        );
        Long postId = communityService.createPost(post);

        // 2. 첨부파일 영구화 및 매핑
        List<Long> fileUploadIds = request.getFileUploadIds();
        if (fileUploadIds != null && !fileUploadIds.isEmpty()) {
            int order = 0;
            for (Long fileUploadId : fileUploadIds) {
                FileUpload fileUpload = fileUploadRepository.findById(fileUploadId)
                        .orElseThrow(() -> new ImageException(com.cheatkey.common.exception.ErrorCode.FILE_NOT_FOUND));

                if (fileUpload.getIsTemp()) {
                    String permanentKey = fileUpload.getS3Key().replaceFirst("uploads/temp/", "uploads/");
                    s3FileService.moveToPermanent(fileUpload.getS3Key(), permanentKey);

                    fileUpload.updateToPermanent(permanentKey);
                    fileUploadRepository.save(fileUpload);
                }

                CommunityPostFile fileEntity = CommunityPostFile.builder()
                        .postId(postId)
                        .fileUploadId(fileUpload.getId())
                        .sortOrder(order++)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                communityPostFileRepository.save(fileEntity);
            }
        }
        return postId;
    }
} 