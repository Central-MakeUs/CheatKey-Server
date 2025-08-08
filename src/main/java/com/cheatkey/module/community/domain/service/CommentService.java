package com.cheatkey.module.community.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.domain.repository.CommunityCommentRepository;
import com.cheatkey.module.community.domain.repository.CommunityPostRepository;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;

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

            if (parent.getStatus() != CommentStatus.ACTIVE) {
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
        
        return commentRepository.findByPostIdAndStatus(postId, CommentStatus.ACTIVE);
    }
}
