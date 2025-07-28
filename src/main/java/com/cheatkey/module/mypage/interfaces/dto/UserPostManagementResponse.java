package com.cheatkey.module.mypage.interfaces.dto;

import com.cheatkey.common.interfaces.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "사용자 작성글 관리 응답")
public class UserPostManagementResponse extends PageResponse<UserPostResponse> {
    
    @Schema(description = "작성글 목록")
    private List<UserPostResponse> posts;
    
    @Schema(description = "전체 작성글 수")
    private long totalPosts;
    
    public UserPostManagementResponse(int currentPage, int pageSize, List<UserPostResponse> posts, long totalPosts) {
        super(currentPage, pageSize);
        this.posts = posts;
        this.totalPosts = totalPosts;
    }
} 