package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.mapper.CommunityMapper;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityMapper communityMapper;
    private final CommunityService communityService;


    // 커뮤니티 글 등록
    @PostMapping("/posts")
    public ResponseEntity<Long> createPost(@RequestBody CommunityPostCreateRequest request) {

        CommunityPost communityPost = communityMapper.toCommunityPost(request);
        Long postId = communityService.createPost(communityPost);
        return ResponseEntity.ok(postId);
    }
}
