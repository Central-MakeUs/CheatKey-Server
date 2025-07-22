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

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityReportedPostRepository communityReportedPostRepository;
    private final CommunityPostBlockRepository communityPostBlockRepository;

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
}
