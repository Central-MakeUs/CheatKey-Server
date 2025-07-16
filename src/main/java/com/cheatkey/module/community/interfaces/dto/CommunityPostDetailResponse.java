package com.cheatkey.module.community.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommunityPostDetailResponse {
    private Long id;
    private String title;
    private String content;
    private String authorNickname;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private int commentCount;
    // 댓글/대댓글 리스트는 별도
}
