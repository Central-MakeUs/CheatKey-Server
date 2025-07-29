package com.cheatkey.module.community.domian.service;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.CommunityReportedPost;
import com.cheatkey.module.community.domian.entity.CommunityPostBlock;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostBlockRepository;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cheatkey.module.community.domian.entity.CommunityPostFile;
import com.cheatkey.module.community.domian.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import org.springframework.data.domain.PageImpl;
import com.cheatkey.module.mypage.interfaces.dto.UserPostResponse;
import com.cheatkey.module.community.domian.entity.mapper.CommunityPostMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import com.cheatkey.module.community.domian.repository.CommunityCommentRepository;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityReportedPostRepository communityReportedPostRepository;
    private final CommunityPostBlockRepository communityPostBlockRepository;
    private final CommunityPostFileRepository communityPostFileRepository;
    private final FileUploadRepository fileUploadRepository;
    private final CommunityCommentRepository communityCommentRepository;

    private final FileService fileService;
    private final CommentService commentService;

    private final CommunityPostMapper communityPostMapper;

    /**
     * 커뮤니티 게시글 목록 조회 (ACTIVE 상태, 차단 제외, 페이징, 검색, 정렬)
     */
    public Page<CommunityPostListResponse> getPostList(Long userId, String keyword, String sort, Pageable pageable) {

        // 1. QueryDSL로 ACTIVE, 검색, 정렬, 페이징 한 번에 조회
        Page<CommunityPost> postPage = communityPostRepository.findAllByCustomConditions(userId, keyword, sort, pageable);
        List<Long> postIds = postPage.getContent().stream().map(CommunityPost::getId).toList();

        // 2. 차단된 유저의 게시글 제외 (in절 최적화)
        List<Long> blockedUserIds = communityPostBlockRepository.findAll().stream()
                .filter(b -> b.getBlockerId().equals(userId) && b.getIsActive())
                .map(b -> b.getBlockedId())
                .toList();

        List<CommunityPost> filteredPosts = postPage.getContent().stream()
                .filter(post -> post.getStatus() == PostStatus.ACTIVE)
                .filter(post -> !blockedUserIds.contains(post.getUserId()))
                .toList();

        // 3. 댓글 수 매핑
        Map<Long, Integer> commentCountMap = communityCommentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        // 4. 인기순 정렬(댓글수 기준)
        if ("popular".equals(sort)) {
            filteredPosts = filteredPosts.stream()
                    .sorted((a, b) -> commentCountMap.getOrDefault(b.getId(), 0) - commentCountMap.getOrDefault(a.getId(), 0))
                    .toList();
        }

        // 5. DTO 매핑
        List<CommunityPostListResponse> dtoList = filteredPosts.stream().map(post -> {
            List<CommunityPostFile> files = communityPostFileRepository.findAll().stream()
                    .filter(f -> f.getPostId().equals(post.getId()))
                    .sorted((a, b) -> a.getSortOrder() - b.getSortOrder())
                    .limit(5)
                    .toList();

            List<String> thumbnailUrls = files.stream().map(f -> {
                Optional<FileUpload> fileUploadOpt = fileUploadRepository.findById(f.getFileUploadId());
                return fileUploadOpt.map(fileUpload -> {
                    try {
                        return fileService.getPermanentFilePresignedUrl(fileUpload.getS3Key(), 10).toString();
                    } catch (Exception e) {
                        return null;
                    }
                }).orElse(null);
            }).filter(url -> url != null).collect(Collectors.toList());

            int commentCount = commentCountMap.getOrDefault(post.getId(), 0);
            return communityPostMapper.toListDto(post, commentCount, thumbnailUrls);
        }).toList();

        return new PageImpl<>(dtoList, pageable, postPage.getTotalElements());
    }

    /**
     * 커뮤니티 게시글 상세 조회 (ACTIVE 상태, 차단/신고 정책 반영)
     */
    public CommunityPostDetailResponse getPostDetail(Long userId, Long postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        boolean blocked = communityPostBlockRepository.findAll().stream()
                .anyMatch(b -> b.getBlockerId().equals(userId) && b.getBlockedId().equals(post.getUserId()) && b.getIsActive());

        // 본인 글이 삭제(DELETED) 상태면 상세 조회 불가
        if (post.getUserId().equals(userId) && post.getStatus() == PostStatus.DELETED) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 본인 글이 신고(REPORTED) 상태면 접근 권한 없음 메시지 반환
        if (post.getUserId().equals(userId) && post.getStatus() == PostStatus.REPORTED) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "신고된 게시글에 대한 접근 권한이 없습니다.");
        }

        // 비정상(삭제/신고 등) 상태는 본인 글이 아니면 접근 권한 없음 메시지 반환
        if (post.getStatus() != PostStatus.ACTIVE && !post.getUserId().equals(userId)) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "신고된 게시글에 대한 접근 권한이 없습니다.");
        }

        // 차단된 유저의 글은 차단 메시지 반환 (viewCount 증가 X)
        if (blocked) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "차단된 글입니다.");
        }

        // 조회수 증가 (본인 글이 아닐 때만, 차단/비정상 상태가 아닌 경우에만)
        if (!post.getUserId().equals(userId)) {
            if (post.getViewCount() == null) post.setViewCount(0L);
            post.setViewCount(post.getViewCount() + 1);
            communityPostRepository.save(post);
        }

        List<CommunityPostFile> files = communityPostFileRepository.findAll().stream()
                .filter(f -> f.getPostId().equals(post.getId()))
                .sorted((a, b) -> a.getSortOrder() - b.getSortOrder())
                .toList();

        List<FileUpload> fileUploads = files.stream()
                .map(f -> fileUploadRepository.findById(f.getFileUploadId()).orElse(null))
                .filter(f -> f != null)
                .toList();

        List<com.cheatkey.module.file.interfaces.dto.FileUploadResponse> fileResponses = fileUploads.stream().map(f ->
                com.cheatkey.module.file.interfaces.dto.FileUploadResponse.builder()
                        .id(f.getId())
                        .originalName(f.getOriginalName())
                        .s3Key(f.getS3Key())
                        .size(f.getSize())
                        .contentType(f.getContentType())
                        .isTemp(f.getIsTemp())
                        .createdAt(f.getCreatedAt())
                        .fileUploadId(f.getId())
                        .build()
        ).collect(Collectors.toList());

        List<CommunityCommentResponse> comments = commentService.getCommentsForPost(postId);
        boolean canDelete = post.getUserId().equals(userId);

        return communityPostMapper.toDetailDto(
                post,
                comments.size(),
                fileResponses,
                comments,
                canDelete,
                blocked,
                blocked ? "차단된 글입니다." : null
        );
    }

    /**
     * 마이페이지 - 사용자 작성글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CommunityPost> getUserPosts(Long userId, Pageable pageable) {
        return communityPostRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PostStatus.ACTIVE, pageable);
    }

    /**
     * 댓글 수 맵 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> getCommentCountMap(List<Long> postIds) {
        return communityCommentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));
    }

    /**
     * 게시글 이미지 URL 목록 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> getPostImageUrlsMap(List<Long> postIds) {
        List<CommunityPostFile> files = communityPostFileRepository.findByPostIdIn(postIds);
        List<Long> fileUploadIds = files.stream()
                .map(CommunityPostFile::getFileUploadId)
                .distinct()
                .toList();
        
        Map<Long, FileUpload> fileUploadMap = fileUploadRepository.findAllById(fileUploadIds).stream()
                .collect(Collectors.toMap(FileUpload::getId, f -> f));

        return files.stream()
                .collect(Collectors.groupingBy(
                        CommunityPostFile::getPostId,
                        Collectors.mapping(f -> {
                            FileUpload fileUpload = fileUploadMap.get(f.getFileUploadId());
                            if (fileUpload != null) {
                                try {
                                    return fileService.getPermanentFilePresignedUrl(fileUpload.getS3Key(), 10).toString();
                                } catch (Exception e) {
                                    return null;
                                }
                            }
                            return null;
                        }, Collectors.toList())
                ));
    }

    public Long createPost(CommunityPost communityPost) {
        communityPostRepository.save(communityPost);
        return communityPost.getId();
    }

    @Transactional
    public void reportPost(Long postId, Long reporterId, String reasonCode) {
        if (communityReportedPostRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new CustomException(ErrorCode.POST_ALREADY_REPORTED);
        }
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post = setPostStatus(post, PostStatus.REPORTED);
        communityPostRepository.save(post);
        CommunityReportedPost report = CommunityReportedPost.builder()
                .postId(postId)
                .reporterId(reporterId)
                .reasonCode(reasonCode)
                .build();
        communityReportedPostRepository.save(report);
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId, String reason) {
        if (communityPostBlockRepository.existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId)) {
            throw new CustomException(ErrorCode.USER_ALREADY_BLOCKED);
        }
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .reason(reason)
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_NOT_OWNER);
        }
        
        // 신고된 게시글 삭제 불가
        if (post.getStatus() == PostStatus.REPORTED) {
            throw new CustomException(ErrorCode.POST_REPORTED_ACCESS_DENIED);
        }
        
        post = setPostStatus(post, PostStatus.DELETED);
        communityPostRepository.save(post);
    }

    private CommunityPost setPostStatus(CommunityPost post, PostStatus status) {
        return CommunityPost.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .userId(post.getUserId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .status(status)
                .build();
    }

    /**
     * 인기글 조회 (댓글 수 기준, 최대 limit개)
     * TODO: 페이징 처리로 변경 검토 중 (현재는 성능상 limit 처리 유지)
     */
    @Transactional(readOnly = true)
    public List<CommunityPost> getPopularPosts(int limit) {
        // 1. 모든 ACTIVE 게시글 조회
        List<CommunityPost> allPosts = communityPostRepository.findAll().stream()
                .filter(post -> post.getStatus() == PostStatus.ACTIVE)
                .toList();
        
        // 2. 댓글 수 조회
        List<Long> postIds = allPosts.stream().map(CommunityPost::getId).toList();
        Map<Long, Integer> commentCountMap = communityCommentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        // 3. 댓글 수 기준으로 정렬 후 limit
        return allPosts.stream()
                .sorted((a, b) -> commentCountMap.getOrDefault(b.getId(), 0) - commentCountMap.getOrDefault(a.getId(), 0))
                .limit(limit)
                .toList();
    }
}
