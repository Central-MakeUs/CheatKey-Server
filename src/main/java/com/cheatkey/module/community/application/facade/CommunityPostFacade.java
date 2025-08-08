package com.cheatkey.module.community.application.facade;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityCategory;
import com.cheatkey.module.community.domain.entity.CommunityPostFile;
import com.cheatkey.module.community.domain.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.domain.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import com.cheatkey.module.detection.domain.entity.DetectionCategory;
import com.cheatkey.module.detection.infra.client.VectorDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostFacade {

    private final S3FileService s3FileService;
    private final CommunityService communityService;
    private final FileUploadRepository fileUploadRepository;
    private final CommunityPostFileRepository communityPostFileRepository;
    private final VectorDbClient vectorDbClient;

    @Transactional
    public Long createPostWithFiles(CommunityPostCreateRequest request, Long userId, String nickname) throws ImageException {
        // 1. 커뮤니티 글 등록
        String safeTitle = Jsoup.clean(request.getTitle(), Safelist.none());
        String safeContent = Jsoup.clean(request.getContent(), Safelist.basic());
        CommunityPost post = CommunityPost.createPost(
            safeTitle,
            safeContent,
            request.getCategory(),
            userId,
            nickname
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

        // 3. Vector DB 저장 (REPORT, SHARE 카테고리만)
        if (request.getCategory() == CommunityCategory.REPORT || 
            request.getCategory() == CommunityCategory.SHARE) {
            try {
                String content = safeTitle + " " + safeContent;
                List<Float> embedding = vectorDbClient.embed(content);
                
                Map<String, Object> payload = Map.of(
                    "postId", postId,
                    "category", DetectionCategory.INVESTMENT,
                    "title", safeTitle,
                    "content", safeContent,
                    "userId", userId,
                    "nickname", nickname,
                    "source", "community"
                );
                
                vectorDbClient.saveVector(postId.toString(), embedding, payload);
                log.info("Vector DB 저장 완료: postId={}, category={}", postId, "거래 사기");
            } catch (Exception e) {
                log.warn("Vector DB 저장 실패: postId={}, error={}", postId, e.getMessage());
                // Vector DB 저장 실패해도 게시글 작성은 성공으로 처리
            }
        }
        
        return postId;
    }
} 