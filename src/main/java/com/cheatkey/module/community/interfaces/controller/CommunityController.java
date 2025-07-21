package com.cheatkey.module.community.interfaces.controller;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.community.application.facade.CommunityPostFacade;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "커뮤니티 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityPostFacade communityPostFacade;

    @Operation(summary = "(★) 커뮤니티 글 작성", description = "커뮤니티 글을 작성합니다. 파일 첨부 가능.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "글 작성 성공, postId 반환", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패 등", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = com.cheatkey.common.exception.ErrorResponse.class)))
    })
    @PostMapping("/posts")
    public ResponseEntity<Long> createPost(@Valid @RequestBody CommunityPostCreateRequest request) throws ImageException {
        Long postId = communityPostFacade.createPostWithFiles(request);
        return ResponseEntity.ok(postId);
    }
}
