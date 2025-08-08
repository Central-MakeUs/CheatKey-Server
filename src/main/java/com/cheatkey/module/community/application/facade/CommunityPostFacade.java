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
import com.cheatkey.common.exception.ErrorCode;

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
    public Long createPostWithFiles(CommunityPostCreateRequest request, Long authorId, String authorNickname) throws ImageException {
        // 1. 커뮤니티 글 등록
        String safeTitle = Jsoup.clean(request.getTitle(), Safelist.none());
        String safeContent = Jsoup.clean(request.getContent(), Safelist.basic());
        CommunityPost post = CommunityPost.createPost(
            safeTitle,
            safeContent,
            request.getCategory(),
            authorId,
            authorNickname
        );
        Long postId = communityService.createPost(post);

        // 2. 첨부파일 영구화 및 매핑
        List<Long> fileUploadIds = request.getFileUploadIds();
        
        if (fileUploadIds != null && !fileUploadIds.isEmpty()) {
            log.info("파일 영구화 처리 시작: {}개 파일", fileUploadIds.size());
            for (int i = 0; i < fileUploadIds.size(); i++) {
                Long fileUploadId = fileUploadIds.get(i);
                try {
                    FileUpload fileUpload = fileUploadRepository.findById(fileUploadId)
                            .orElseThrow(() -> new ImageException(ErrorCode.FILE_NOT_FOUND));

                    // 임시 파일을 영구 파일로 이동
                    if (fileUpload.getIsTemp()) {
                        String newKey = fileUpload.getS3Key().replaceFirst("uploads/temp/", "uploads/");
                        
                        try {
                            // S3 파일 이동
                            s3FileService.moveToPermanent(fileUpload.getS3Key(), newKey);
                            
                            // DB 업데이트
                            fileUpload.updateToPermanent(newKey);
                            fileUploadRepository.save(fileUpload);
                            
                            log.info("파일 영구화 완료: fileUploadId={}, oldKey={}, newKey={}", 
                                    fileUploadId, fileUpload.getS3Key(), newKey);
                                    
                        } catch (Exception e) {
                            log.error("파일 영구화 실패: fileUploadId={}, oldKey={}, newKey={}, error={}", 
                                    fileUploadId, fileUpload.getS3Key(), newKey, e.getMessage(), e);
                            throw e;
                        }
                    }

                    // 커뮤니티 게시글과 파일 연결 (순서대로 sortOrder 설정)
                    CommunityPostFile communityPostFile = CommunityPostFile.builder()
                            .postId(postId)
                            .fileUploadId(fileUploadId)
                            .sortOrder(i) // 파일 업로드 순서대로 sortOrder 설정
                            .build();
                    communityPostFileRepository.save(communityPostFile);
                } catch (Exception e) {
                    log.error("파일 처리 중 오류 발생: fileUploadId={}, error={}", fileUploadId, e.getMessage(), e);
                    throw e;
                }
            }
        }

        // TODO: Vector DB 저장 기능 임시 비활성화 (오류 해결 필요)
        // 3. Vector DB 저장 (REPORT, SHARE 카테고리만)
        /*
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
                    "userId", authorId,
                    "nickname", authorNickname,
                    "source", "community"
                );
                
                vectorDbClient.saveVector(postId.toString(), embedding, payload);
                log.info("Vector DB 저장 완료: postId={}", postId);
            } catch (Exception e) {
                log.error("Vector DB 저장 실패: postId={}, error={}", postId, e.getMessage(), e);
                // Vector DB 저장 실패해도 게시글 작성은 성공으로 처리
            }
        }
        */
        
        return postId;
    }
} 