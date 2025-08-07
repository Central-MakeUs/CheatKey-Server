package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostBlockRepository extends JpaRepository<CommunityPostBlock, Long> {
    boolean existsByBlockerIdAndBlockedIdAndIsActiveTrue(Long blockerId, Long blockedId);
} 