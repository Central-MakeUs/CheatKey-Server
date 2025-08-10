package com.cheatkey.module.community.interfaces.dto;

import com.cheatkey.module.community.domain.entity.CommunityCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "커뮤니티 게시글 생성 요청")
public class CommunityPostCreateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해 주세요.")
    @Schema(description = "게시글 제목", example = "사기 사례를 공유합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 10, max = 500, message = "내용은 10자 이상 500자 이하로 입력해 주세요.")
    @Schema(description = "게시글 내용", example = "최근에 겪은 사기 사례를 공유합니다. 주의하세요!")
    private String content;

    @NotNull(message = "카테고리를 선택해 주세요.")
    @Schema(description = "게시글 카테고리", example = "REPORT", allowableValues = {"REPORT", "TALK", "SHARE"})
    private CommunityCategory category;

    @Size(max = 5, message = "이미지는 최대 5개까지 등록할 수 있습니다.")
    @Schema(description = "업로드된 파일 ID 리스트", example = "[1, 2, 3]")
    private List<Long> fileUploadIds; // 업로드된 파일의 id 리스트
}
