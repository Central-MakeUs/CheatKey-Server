package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityReportedComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityReportedCommentRepository extends JpaRepository<CommunityReportedComment, Long> {
    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);
}
