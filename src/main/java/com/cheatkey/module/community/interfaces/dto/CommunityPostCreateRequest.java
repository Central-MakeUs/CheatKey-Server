package com.cheatkey.module.community.interfaces.dto;

import com.cheatkey.module.community.domian.entity.CommunityCategory;
import lombok.Getter;

import java.util.List;

@Getter
public class CommunityPostCreateRequest {
    private Long userId;
    private String title;
    private String content;
    private CommunityCategory category;
    private List<String> imageUrls; // S3 업로드 후 URL 리스트
}
