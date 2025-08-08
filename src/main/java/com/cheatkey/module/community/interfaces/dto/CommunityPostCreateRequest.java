package com.cheatkey.module.community.interfaces.dto;

import com.cheatkey.module.community.domain.entity.CommunityCategory;
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
public class CommunityPostCreateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 10, max = 100, message = "제목은 10자 이상 100자 이하로 입력해 주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 10, max = 500, message = "내용은 10자 이상 500자 이하로 입력해 주세요.")
    private String content;

    @NotNull(message = "카테고리를 선택해 주세요.")
    private CommunityCategory category;

    @Size(max = 5, message = "이미지는 최대 5개까지 등록할 수 있습니다.")
    private List<Long> fileUploadIds; // 업로드된 파일의 id 리스트
}
