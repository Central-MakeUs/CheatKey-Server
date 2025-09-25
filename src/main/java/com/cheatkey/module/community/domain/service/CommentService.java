package com.cheatkey.module.community.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domain.entity.CommunityReportedComment;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.domain.repository.CommunityReportedCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import com.cheatkey.module.community.domain.entity.mapper.CommunityPostMapper;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityReportedCommentRepository communityReportedCommentRepository;
    private final CommunityPostBlockRepository communityPostBlockRepository;
    private final CommunityPostMapper communityPostMapper;
    private final WithdrawnUserCacheService withdrawnUserCacheService;

    @Transactional
    public Long createComment(CommunityCommentRequest request, Long authorId, String authorNickname) {
        CommunityPost post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        if (post.getStatus() != null && !post.getStatus().name().equals("ACTIVE")) {
            throw new CustomException(ErrorCode.COMMUNITY_POST_DELETED_OR_REPORTED);
        }

        CommunityComment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_COMMENT_PARENT_NOT_FOUND));

            if (parent.getParent() != null) {
                throw new CustomException(ErrorCode.COMMUNITY_COMMENT_CANNOT_REPLY_TO_REPLY);
            }

            if (parent.getStatus() != null && !parent.getStatus().name().equals("ACTIVE")) {
                throw new CustomException(ErrorCode.COMMUNITY_COMMENT_DELETED_CANNOT_REPLY);
            }
        }

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(parent)
                .authorId(authorId)
                .authorNickname(authorNickname)
                .content(request.getContent())
                .status(CommentStatus.ACTIVE)
                .build();
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public void deleteComment(Long commentId, Long authorId) {
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        if (!comment.getAuthorId().equals(authorId)) {
            throw new CustomException(ErrorCode.COMMUNITY_COMMENT_ONLY_AUTHOR_CAN_DELETE);
        }
        comment.setStatus(CommentStatus.DELETED);
        comment.setDeletedAt(java.time.LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommunityComment> getCommentsForPost(Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        if (post.getStatus() != null && !post.getStatus().name().equals("ACTIVE")) {
            throw new CustomException(ErrorCode.COMMUNITY_POST_DELETED_OR_REPORTED);
        }
        
        // 모든 상태의 댓글과 대댓글 조회 (ACTIVE, DELETED 모두 포함)
        List<CommunityComment> allComments = commentRepository.findByPostId(postId);
        
        // 탈퇴한 사용자 처리 - 닉네임만 "(알수없음)"로 변경
        List<Long> withdrawnUserIds = withdrawnUserCacheService.getWithdrawnUserIds();
        return allComments.stream()
                .map(comment -> {
                    if (withdrawnUserIds.contains(comment.getAuthorId())) {
                        // 탈퇴한 사용자의 댓글은 닉네임만 "(알수없음)"로 표기
                        return CommunityComment.builder()
                                .id(comment.getId())
                                .post(comment.getPost())
                                .parent(comment.getParent())
                                .authorId(comment.getAuthorId())
                                .authorNickname("(알수없음)")
                                .content(comment.getContent())
                                .status(comment.getStatus())
                                .createdAt(comment.getCreatedAt())
                                .deletedAt(comment.getDeletedAt())
                                .build();
                    }
                    return comment;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getCommentsForPostWithBlocking(Long postId, Long currentUserId) {
        // 1. 댓글 조회
        List<CommunityComment> comments = getCommentsForPost(postId);
        
        // 2. 차단된 사용자 ID 조회
        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);
        
        // 3. 댓글 트리 구성
        Map<Long, List<CommunityComment>> childrenMap = buildCommentTree(comments);
        
        // 4. 댓글 처리 (기존 toCommentDtoList 로직)
        return processComments(comments, childrenMap, currentUserId, blockedUserIds);
    }

    private List<Long> getBlockedUserIds(Long currentUserId) {
        return communityPostBlockRepository
            .findByBlockerIdAndIsActive(currentUserId, true)
            .stream()
            .map(CommunityPostBlock::getBlockedId)
            .toList();
    }

    private Map<Long, List<CommunityComment>> buildCommentTree(List<CommunityComment> comments) {
        return comments.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));
    }

    private List<CommunityCommentResponse> processComments(List<CommunityComment> comments, Map<Long, List<CommunityComment>> childrenMap, Long currentUserId, List<Long> blockedUserIds) {
        List<CommunityCommentResponse> result = new ArrayList<>();
        
        // ACTIVE 댓글들
        List<CommunityCommentResponse> activeComments = comments.stream()
            .filter(c -> c.getParent() == null && c.getStatus() == CommentStatus.ACTIVE)
            .map(c -> processComment(c, childrenMap, currentUserId, blockedUserIds))
            .toList();
        result.addAll(activeComments);
        
        // REPORTED 댓글들
        List<CommunityCommentResponse> reportedComments = comments.stream()
            .filter(c -> c.getParent() == null && c.getStatus() == CommentStatus.REPORTED)
            .map(c -> processComment(c, childrenMap, currentUserId, blockedUserIds))
            .toList();
        result.addAll(reportedComments);
        
        // DELETED 댓글들 (고아 대댓글)
        List<CommunityCommentResponse> deletedComments = comments.stream()
            .filter(c -> c.getParent() == null && c.getStatus() == CommentStatus.DELETED)
            .filter(c -> childrenMap.containsKey(c.getId()) && !childrenMap.get(c.getId()).isEmpty())
            .map(c -> processComment(c, childrenMap, currentUserId, blockedUserIds))
            .toList();
        result.addAll(deletedComments);
        
        return result;
    }

    private CommunityCommentResponse processComment(CommunityComment comment, Map<Long, List<CommunityComment>> childrenMap, Long currentUserId, List<Long> blockedUserIds) {
        // 차단된 사용자 댓글 처리 (가장 우선순위)
        if (blockedUserIds.contains(comment.getAuthorId())) {
            // 부모 댓글이 차단되면 자식 댓글은 빈 배열로 처리 (미노출)
            return communityPostMapper.toBlockedCommentDto(comment, List.of());
        }

        // 대댓글들 처리 (부모 댓글이 차단되지 않은 경우만)
        List<CommunityCommentResponse> children = childrenMap.getOrDefault(comment.getId(), List.of()).stream()
            .map(child -> processComment(child, childrenMap, currentUserId, blockedUserIds))
            .collect(Collectors.toList());

        // 삭제된 댓글 처리
        if (comment.getStatus() == CommentStatus.DELETED) {
            return communityPostMapper.toDeletedCommentDto(comment, children);
        }

        // 신고된 댓글 처리
        if (comment.getStatus() == CommentStatus.REPORTED) {
            return communityPostMapper.toReportedCommentDto(comment, children);
        }

        // 정상 댓글 처리
        return communityPostMapper.toNormalCommentDto(comment, children, currentUserId);
    }

    @Transactional
    public void reportComment(Long commentId, Long reporterId, String reasonCode) {
        if (communityReportedCommentRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_REPORTED);
        }
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        
        // 본인이 작성한 댓글은 신고할 수 없음
        if (comment.getAuthorId().equals(reporterId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_OWN_COMMENT);
        }
        
        // 신고 기록 저장
        CommunityReportedComment report = CommunityReportedComment.builder()
                .commentId(commentId)
                .reporterId(reporterId)
                .reasonCode(reasonCode)
                .build();
        communityReportedCommentRepository.save(report);
        
        // 신고 횟수 체크 후 상태 변경 (2회 신고 시에만 적용)
        long reportCount = communityReportedCommentRepository.countByCommentId(commentId);
        if (reportCount == 2) {
            comment.setStatus(CommentStatus.REPORTED);
            commentRepository.save(comment);
        }
    }
}
