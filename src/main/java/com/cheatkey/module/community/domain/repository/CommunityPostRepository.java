package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, QuerydslPredicateExecutor<CommunityPost>, CommunityPostRepositoryCustom {
    // 페이징, 검색, 정렬 등 커스텀 쿼리 필요시 QueryDSL 사용
    Page<CommunityPost> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PostStatus status, Pageable pageable);
}

interface CommunityPostRepositoryCustom {
    Page<CommunityPost> findAllByCustomConditions(Long userId, String keyword, String sort, Pageable pageable);
}
