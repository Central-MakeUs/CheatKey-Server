package com.cheatkey.module.community.domian.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domian.entity.comment.CommunityComment;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.comment.CommentStatus;
import com.cheatkey.module.community.domian.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;

    @Transactional
    public Long createComment(CommunityCommentRequest request, Long userId, String userNickname) {
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

            if (parent.getStatus() != CommentStatus.ACTIVE) {
                throw new CustomException(ErrorCode.COMMUNITY_COMMENT_DELETED_CANNOT_REPLY);
            }
        }

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(parent)
                .userId(userId)
                .userNickname(userNickname)
                .content(request.getContent())
                .status(CommentStatus.ACTIVE)
                .build();
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        if (!comment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMUNITY_COMMENT_ONLY_AUTHOR_CAN_DELETE);
        }
        comment.setStatus(CommentStatus.DELETED);
        comment.setDeletedAt(java.time.LocalDateTime.now());
        commentRepository.save(comment);
    }

    // @TODO CommunityCommentResponse DTO 객체 서비스에서 미사용 구현으로 수정
    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getCommentsForPost(Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        if (post.getStatus() != null && !post.getStatus().name().equals("ACTIVE")) {
            throw new CustomException(ErrorCode.COMMUNITY_POST_DELETED_OR_REPORTED);
        }
        List<CommunityComment> allComments = commentRepository.findAll().stream()
                .filter(c -> c.getPost().getId().equals(postId))
                .filter(c -> c.getStatus() == CommentStatus.ACTIVE)
                .toList();

        // 댓글/대댓글 트리 구성
        Map<Long, List<CommunityComment>> childrenMap = allComments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        List<CommunityCommentResponse> result = allComments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> toResponseWithChildren(c, childrenMap))
                .collect(Collectors.toList());
        return result;
    }

    private CommunityCommentResponse toResponseWithChildren(CommunityComment comment, Map<Long, List<CommunityComment>> childrenMap) {
        List<CommunityCommentResponse> children = childrenMap.getOrDefault(comment.getId(), List.of()).stream()
                .map(child -> toResponseWithChildren(child, childrenMap))
                .collect(Collectors.toList());
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .userId(comment.getUserId())
                .userNickname(comment.getUserNickname())
                .content(comment.getContent())
                .status(comment.getStatus().name())
                .createdAt(comment.getCreatedAt())
                .children(children)
                .build();
    }
}
