package com.cheatkey.module.home.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "메인 대시보드 응답")
public class HomeDashboardResponse {

    @Schema(description = "사용자 정보")
    private UserInfo userInfo;

    @Schema(description = "인기글 목록")
    private List<PopularPost> popularPosts;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "사용자 정보")
    public static class UserInfo {
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.../profile.jpg")
        private String profileImageUrl;

        @Schema(description = "사용자 레벨 (1로 고정)", example = "1")
        private Integer level;

        @Schema(description = "닉네임", example = "테스트")
        private String nickname;

        @Schema(description = "총 방문 횟수", example = "0")
        private Integer totalVisitCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "인기글 정보")
    public static class PopularPost {
        @Schema(description = "게시글 ID", example = "1")
        private Long id;

        @Schema(description = "게시글 제목", example = "택배 지연 문자 조심!!")
        private String title;

        @Schema(description = "게시글 내용 미리보기", example = "택배 지연 문자를 클릭하자 악성 앱이 설치되어 개인 정보가 유출됐어요. 조심하세요택배 지연 문자를..")
        private String content;

        @Schema(description = "작성자 닉네임", example = "사기꾼")
        private String authorNickname;
    }
} 