package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.community.application.facade.CommunityPostFacade;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityPostFacade communityPostFacade;

    @PostMapping("/posts")
    public ResponseEntity<Long> createPost(@Valid @RequestBody CommunityPostCreateRequest request) throws ImageException {
        Long postId = communityPostFacade.createPostWithFiles(request);
        return ResponseEntity.ok(postId);
    }
}
