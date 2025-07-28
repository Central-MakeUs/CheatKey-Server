package com.cheatkey.module.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "마이페이지 대시보드 응답")
public class MyPageDashboardResponse {
    
    @Schema(description = "사용자 정보")
    private UserInfoResponse userInfo;
    
    @Schema(description = "프로필 이미지 목록")
    private List<ProfileImageResponse> profileImages;
} 