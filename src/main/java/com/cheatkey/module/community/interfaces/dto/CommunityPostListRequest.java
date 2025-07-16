package com.cheatkey.module.community.interfaces.dto;

import com.cheatkey.module.community.domian.entity.CommunityCategory;

public class CommunityPostListRequest {
    private CommunityCategory category;
    private String keyword; // 제목/내용 검색
    private String sort; // 최신순, 인기순
}
