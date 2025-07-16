package com.cheatkey.module.community.domian.service;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;

    public Long createPost(CommunityPost communityPost) {
        CommunityPost post = CommunityPost.createPost(communityPost);
        communityPostRepository.save(post);
        return post.getId();
    }
}
