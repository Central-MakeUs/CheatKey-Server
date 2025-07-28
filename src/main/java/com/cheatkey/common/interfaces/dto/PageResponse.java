package com.cheatkey.common.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Schema(description = "페이징 정보")
public class PageResponse<T> {
    
    @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
    private int currentPage;
    
    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;
    
    public PageResponse(int currentPage, int pageSize) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }
    
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getNumber(), page.getSize());
    }
} 