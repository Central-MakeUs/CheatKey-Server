package com.cheatkey.module.community.domian.repository;

import com.cheatkey.module.community.domian.entity.CommunityReportedPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityReportedPostRepository extends JpaRepository<CommunityReportedPost, Long> {
    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);
} 