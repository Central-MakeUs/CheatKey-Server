package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, QuerydslPredicateExecutor<CommunityComment> {
    @Query("SELECT c.post.id as postId, COUNT(c) as count FROM CommunityComment c WHERE c.post.id IN :postIds AND c.status = 'ACTIVE' GROUP BY c.post.id")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);
    
    List<CommunityComment> findByPostIdAndStatus(Long postId, CommentStatus status);
}
