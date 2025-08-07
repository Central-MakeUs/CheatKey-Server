package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityReportedPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityReportedPostRepository extends JpaRepository<CommunityReportedPost, Long> {
    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);
} 