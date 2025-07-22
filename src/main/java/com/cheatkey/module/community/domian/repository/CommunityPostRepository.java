package com.cheatkey.module.community.domian.repository;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, QuerydslPredicateExecutor<CommunityPost>, CommunityPostRepositoryCustom {
    // 페이징, 검색, 정렬 등 커스텀 쿼리 필요시 QueryDSL 사용
}

interface CommunityPostRepositoryCustom {
    Page<CommunityPost> findAllByCustomConditions(Long userId, String keyword, String sort, Pageable pageable);
}
