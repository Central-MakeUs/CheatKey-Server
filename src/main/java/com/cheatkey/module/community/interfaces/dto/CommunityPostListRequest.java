package com.cheatkey.module.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "커뮤니티 게시글 목록 조회 요청")
public class CommunityPostListRequest {
    @Schema(description = "검색어", example = "사기")
    private String keyword;

    @Schema(description = "카테고리", example = "REPORT", allowableValues = {"REPORT", "TALK", "SHARE"})
    private String category;

    @Schema(description = "정렬 기준(latest: 최신순, popular: 인기순)", example = "latest")
    private String sort = "latest";

    @Schema(description = "페이지 번호(1부터 시작)", example = "1")
    private Integer page = 1;

    @Schema(description = "페이지 크기", example = "20")
    private Integer size = 20;
}
