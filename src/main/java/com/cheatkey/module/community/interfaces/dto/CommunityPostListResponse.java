package com.cheatkey.module.community.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommunityPostListResponse {
    private Long id;
    private String title;
    private String authorNickname;
    private LocalDateTime createdAt;
    private int commentCount;
    private List<String> imageUrls;
}
