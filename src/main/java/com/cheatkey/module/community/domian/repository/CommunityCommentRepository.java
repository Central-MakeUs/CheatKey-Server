package com.cheatkey.module.community.domian.repository;

import com.cheatkey.module.community.domian.entity.comment.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, QuerydslPredicateExecutor<CommunityComment> {
    @Query("SELECT c.post.id, COUNT(c) FROM CommunityComment c WHERE c.post.id IN :postIds AND c.status = 'ACTIVE' GROUP BY c.post.id")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);
}
