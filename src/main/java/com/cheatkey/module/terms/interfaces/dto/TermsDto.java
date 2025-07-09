package com.cheatkey.module.terms.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TermsDto {
    private Long id;
    private String title;
    private boolean required;
    private String content;
    private String version;
}
