package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityPostFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostFileRepository extends JpaRepository<CommunityPostFile, Long> {
    List<CommunityPostFile> findByPostIdIn(List<Long> postIds);
    List<CommunityPostFile> findByPostId(Long postId);
} 