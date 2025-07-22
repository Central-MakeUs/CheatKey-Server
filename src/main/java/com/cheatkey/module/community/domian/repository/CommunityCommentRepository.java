package com.cheatkey.module.community.domian.repository;

import com.cheatkey.module.community.domian.entity.comment.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, QuerydslPredicateExecutor<CommunityComment> {
    // 커스텀 쿼리 필요시 QueryDSL 사용
}
