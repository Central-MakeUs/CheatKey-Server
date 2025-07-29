package com.cheatkey.module.community.domian.repository;

import com.cheatkey.module.community.domian.entity.CommunityPostFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostFileRepository extends JpaRepository<CommunityPostFile, Long> {
    List<CommunityPostFile> findByPostIdIn(List<Long> postIds);
    List<CommunityPostFile> findByPostId(Long postId);
} 