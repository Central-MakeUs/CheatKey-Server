package com.cheatkey.module.community.domain.service;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.CommunityReportedPost;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityReportedPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cheatkey.module.community.domain.entity.CommunityPostFile;
import com.cheatkey.module.community.domain.repository.CommunityPostFileRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import com.cheatkey.module.file.domain.service.FileService;
import org.springframework.data.domain.PageImpl;
import com.cheatkey.module.community.domain.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.auth.domain.entity.AuthStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.service.WithdrawnUserCacheService;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityReportedPostRepository communityReportedPostRepository;
    private final CommunityPostBlockRepository communityPostBlockRepository;
    private final CommunityPostFileRepository communityPostFileRepository;
    private final FileUploadRepository fileUploadRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final AuthRepository authRepository;

    private final FileService fileService;
    private final CommentService commentService;
    private final WithdrawnUserCacheService withdrawnUserCacheService;

    private final CommunityPostMapper communityPostMapper;

    /**
     * 커뮤니티 게시글 목록 조회 (ACTIVE 상태, 차단 제외, 페이징, 검색, 카테고리, 정렬)
     */
    public Page<CommunityPostListResponse> getPostList(Long userId, String keyword, String category, String sort, Pageable pageable) {

        // 1. QueryDSL로 ACTIVE, 검색, 카테고리, 정렬, 페이징 한 번에 조회
        Page<CommunityPost> postPage = communityPostRepository.findAllByCustomConditions(userId, keyword, category, sort, pageable);
        List<Long> postIds = postPage.getContent().stream().map(CommunityPost::getId).toList();

        // 2. 차단된 유저의 게시글 제외 (쿼리 최적화)
        List<Long> blockedUserIds = communityPostBlockRepository.findByBlockerIdAndIsActive(userId, true)
                .stream()
                .map(CommunityPostBlock::getBlockedId)
                .toList();

        List<CommunityPost> filteredPosts = postPage.getContent().stream()
                .filter(post -> post.getStatus() == PostStatus.ACTIVE)
                .filter(post -> !blockedUserIds.contains(post.getAuthorId()))
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
                    .sorted((a, b) -> {
                        // 1차 정렬: 댓글 수 비교
                        int commentCountA = commentCountMap.getOrDefault(a.getId(), 0);
                        int commentCountB = commentCountMap.getOrDefault(b.getId(), 0);
                        if (commentCountA != commentCountB) {
                            return commentCountB - commentCountA; // 내림차순
                        }
                        
                        // 2차 정렬: 조회수 비교
                        Long viewCountA = a.getViewCount() != null ? a.getViewCount() : 0L;
                        Long viewCountB = b.getViewCount() != null ? b.getViewCount() : 0L;
                        if (!viewCountA.equals(viewCountB)) {
                            return viewCountB.compareTo(viewCountA); // 내림차순
                        }
                        
                        // 3차 정렬: 작성일시 비교
                        return b.getCreatedAt().compareTo(a.getCreatedAt()); // 내림차순
                    })
                    .toList();
        }

        // 5. 탈퇴한 사용자 처리
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        List<CommunityPost> processedPosts = filteredPosts.stream()
                .map(post -> {
                    if (withdrawnUserIds.contains(post.getAuthorId())) {
                        // 탈퇴한 사용자의 게시글은 '(알수없음)'로 표기
                        return markAsWithdrawnUser(post);
                    }
                    return post;
                })
                .toList();

        // 6. DTO 변환 (processedPosts 사용)
        List<CommunityPostListResponse> responseList = processedPosts.stream()
                .map(post -> {
                    int commentCount = commentCountMap.getOrDefault(post.getId(), 0);
                    List<String> thumbnailUrls = getPostImageUrlsMap(List.of(post.getId())).getOrDefault(post.getId(), List.of());
                    return communityPostMapper.toListDto(post, commentCount, thumbnailUrls);
                })
                .toList();

        return new PageImpl<>(responseList, pageable, postPage.getTotalElements());
    }

    /**
     * 커뮤니티 게시글 상세 조회 (ACTIVE 상태, 차단/신고 정책 반영)
     */
    @Transactional
    public CommunityPostDetailResponse getPostDetail(Long userId, Long postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Long postAuthorId = post.getAuthorId();
        boolean blocked = communityPostBlockRepository.findByBlockerIdAndIsActive(userId, true).stream()
                .anyMatch(b -> b.getBlockedId().equals(postAuthorId));

        // 본인 글이 삭제(DELETED) 상태면 상세 조회 불가
        if (post.getAuthorId().equals(userId) && post.getStatus() == PostStatus.DELETED) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 본인 글이 신고(REPORTED) 상태면 접근 권한 없음 메시지 반환
        if (post.getAuthorId().equals(userId) && post.getStatus() == PostStatus.REPORTED) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "신고된 게시글에 대한 접근 권한이 없습니다.");
        }

        // 비정상(삭제/신고 등) 상태는 본인 글이 아니면 접근 권한 없음 메시지 반환
        if (post.getStatus() != PostStatus.ACTIVE && !post.getAuthorId().equals(userId)) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "신고된 게시글에 대한 접근 권한이 없습니다.");
        }

        // 차단된 유저의 글은 차단 메시지 반환 (viewCount 증가 X)
        if (blocked) {
            return communityPostMapper.toDetailDto(post, 0, List.of(), List.of(), false, blocked, "차단된 글입니다.");
        }

        // 조회수 증가 (본인 글이 아닐 때만, 차단/비정상 상태가 아닌 경우에만)
        if (!post.getAuthorId().equals(userId)) {
            if (post.getViewCount() == null) post.setViewCount(0L);
            post.setViewCount(post.getViewCount() + 1);
            communityPostRepository.save(post);
        }

        // 탈퇴한 사용자 처리
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        if (withdrawnUserIds.contains(post.getAuthorId())) {
            // 탈퇴한 사용자의 게시글은 '(알수없음)'로 표기
            post = markAsWithdrawnUser(post);
        }

        List<String> presignedUrls = getPostImageUrls(post.getId());

        List<CommunityCommentResponse> commentResponses = commentService.getCommentsForPostWithBlocking(postId, userId);
        int totalCommentCount = communityCommentRepository.countCommentsByPostId(postId).intValue();
        
        boolean canDelete = post.getAuthorId().equals(userId);

        return communityPostMapper.toDetailDto(
                post,
                totalCommentCount,  // commentResponses.size() 대신 전체 댓글 수 사용
                presignedUrls,
                commentResponses,
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
        Page<CommunityPost> postPage = communityPostRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, PostStatus.ACTIVE, pageable);
        
        // 탈퇴한 사용자 처리
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        List<CommunityPost> posts = postPage.getContent().stream()
                .map(post -> {
                    if (withdrawnUserIds.contains(post.getAuthorId())) {
                        // 탈퇴한 사용자의 게시글은 '(알수없음)'로 표기
                        return markAsWithdrawnUser(post);
                    }
                    return post;
                })
                .toList();
        
        return new PageImpl<>(posts, pageable, postPage.getTotalElements());
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
     * 게시글 이미지 URL 목록 조회 (정렬 포함)
     */
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
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                ));
    }

    /**
     * 단일 게시글 이미지 URL 목록 조회 (정렬 포함)
     */
    public List<String> getPostImageUrls(Long postId) {
        List<CommunityPostFile> files = communityPostFileRepository.findByPostIdIn(List.of(postId)).stream()
                .sorted((a, b) -> {
                    Integer sortOrderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
                    Integer sortOrderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
                    return sortOrderA - sortOrderB;
                })
                .toList();

        List<Long> fileUploadIds = files.stream()
                .map(CommunityPostFile::getFileUploadId)
                .distinct()
                .toList();
        
        Map<Long, FileUpload> fileUploadMap = fileUploadRepository.findAllById(fileUploadIds).stream()
                .collect(Collectors.toMap(FileUpload::getId, f -> f));

        return files.stream()
                .map(f -> {
                    FileUpload fileUpload = fileUploadMap.get(f.getFileUploadId());
                    if (fileUpload != null) {
                        try {
                            return fileService.getPermanentFilePresignedUrl(fileUpload.getS3Key(), 10).toString();
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
        
        // 본인이 작성한 게시글은 신고할 수 없음
        if (post.getAuthorId().equals(reporterId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_OWN_POST);
        }
        
        // 신고 기록 저장
        CommunityReportedPost report = CommunityReportedPost.builder()
                .postId(postId)
                .reporterId(reporterId)
                .reasonCode(reasonCode)
                .build();
        communityReportedPostRepository.save(report);
        
        // 신고 횟수 체크 후 상태 변경 (2회 이상 신고 시)
        long reportCount = communityReportedPostRepository.countByPostId(postId);
        if (reportCount >= 2) {
            post.setStatus(PostStatus.REPORTED);
            communityPostRepository.save(post);
        }
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
    public void blockPostAuthor(Long blockerId, Long postId, String reason) {
        // 1. 게시글 존재 여부 확인 및 작성자 ID 조회
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        Long blockedId = post.getAuthorId();
        
        // 2. 자기 자신을 차단하려는 경우 방지
        if (blockerId.equals(blockedId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        
        // 3. 이미 차단한 유저인지 확인
        if (communityPostBlockRepository.existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId)) {
            throw new CustomException(ErrorCode.USER_ALREADY_BLOCKED);
        }
        
        // 4. 차단 처리
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
        if (!post.getAuthorId().equals(userId)) {
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
                .authorId(post.getAuthorId())
                .authorNickname(post.getAuthorNickname())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .status(status)
                .build();
    }

    /**
     * 인기글 조회 (댓글 수 기준, 최대 limit개)
     * 정렬 기준: 1차-댓글수(내림차순), 2차-조회수(내림차순), 3차-작성일시(내림차순)
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

        // 3. 탈퇴한 사용자 처리
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        allPosts = allPosts.stream()
                .map(post -> {
                    if (post.getAuthorId() != null && withdrawnUserIds.contains(post.getAuthorId())) {
                        // 탈퇴한 사용자의 게시글은 '(알수없음)'로 표기
                        return markAsWithdrawnUser(post);
                    }
                    return post;
                })
                .toList();

        // 4. 다중 정렬 기준으로 정렬 후 limit
        // 1차: 댓글 수 (내림차순), 2차: 조회수 (내림차순), 3차: 작성일시 (내림차순)
        return allPosts.stream()
                .sorted((a, b) -> {
                    // 1차 정렬: 댓글 수 비교
                    int commentCountA = commentCountMap.getOrDefault(a.getId(), 0);
                    int commentCountB = commentCountMap.getOrDefault(b.getId(), 0);
                    if (commentCountA != commentCountB) {
                        return commentCountB - commentCountA; // 내림차순
                    }
                    
                    // 2차 정렬: 조회수 비교
                    Long viewCountA = a.getViewCount() != null ? a.getViewCount() : 0L;
                    Long viewCountB = b.getViewCount() != null ? b.getViewCount() : 0L;
                    if (!viewCountA.equals(viewCountB)) {
                        return viewCountB.compareTo(viewCountA); // 내림차순
                    }
                    
                    // 3차 정렬: 작성일시 비교
                    return b.getCreatedAt().compareTo(a.getCreatedAt()); // 내림차순
                })
                .limit(limit)
                .toList();
    }

    /**
     * 인기글 조회 (작성자 프로필 이미지 정보 포함)
     * 정렬 기준: 1차-댓글수(내림차순), 2차-조회수(내림차순), 3차-작성일시(내림차순)
     */
    @Transactional(readOnly = true)
    public List<CommunityPostWithAuthorInfo> getPopularPostsWithAuthorInfo(int limit) {
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

        // 3. 작성자 정보 조회
        List<Long> authorIds = allPosts.stream().map(CommunityPost::getAuthorId).distinct().toList();
        Map<Long, Auth> authorMap = authRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Auth::getId, auth -> auth));

        // 4. 탈퇴한 사용자 처리
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        allPosts = allPosts.stream()
                .map(post -> {
                    if (withdrawnUserIds.contains(post.getAuthorId())) {
                        // 탈퇴한 사용자의 게시글은 '(알수없음)'로 표기
                        return markAsWithdrawnUser(post);
                    }
                    return post;
                })
                .toList();

        // 5. 다중 정렬 기준으로 정렬 후 limit
        // 1차: 댓글 수 (내림차순), 2차: 조회수 (내림차순), 3차: 작성일시 (내림차순)
        return allPosts.stream()
                .sorted((a, b) -> {
                    // 1차 정렬: 댓글 수 비교
                    int commentCountA = commentCountMap.getOrDefault(a.getId(), 0);
                    int commentCountB = commentCountMap.getOrDefault(b.getId(), 0);
                    if (commentCountA != commentCountB) {
                        return commentCountB - commentCountA; // 내림차순
                    }
                    
                    // 2차 정렬: 조회수 비교
                    Long viewCountA = a.getViewCount() != null ? a.getViewCount() : 0L;
                    Long viewCountB = b.getViewCount() != null ? b.getViewCount() : 0L;
                    if (!viewCountA.equals(viewCountB)) {
                        return viewCountB.compareTo(viewCountA); // 내림차순
                    }
                    
                    // 3차 정렬: 작성일시 비교
                    return b.getCreatedAt().compareTo(a.getCreatedAt()); // 내림차순
                })
                .limit(limit)
                .map(post -> {
                    Auth author = authorMap.get(post.getAuthorId());
                    return new CommunityPostWithAuthorInfo(post, author);
                })
                .toList();
    }

    /**
     * 탈퇴한 사용자의 게시글을 '(알수없음)'로 표기하는 공통 메서드
     */
    public CommunityPost markAsWithdrawnUser(CommunityPost post) {
        return CommunityPost.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorId(post.getAuthorId())
                .authorNickname("(알수없음)")
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .status(post.getStatus())
                .build();
    }

    /**
     * 게시글과 작성자 정보를 함께 담는 내부 클래스
     */
    public static class CommunityPostWithAuthorInfo {
        private final CommunityPost post;
        private final Auth author;

        public CommunityPostWithAuthorInfo(CommunityPost post, Auth author) {
            this.post = post;
            this.author = author;
        }

        public CommunityPost getPost() {
            return post;
        }

        public Auth getAuthor() {
            return author;
        }
    }
}
