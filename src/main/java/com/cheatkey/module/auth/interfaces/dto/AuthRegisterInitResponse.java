package com.cheatkey.module.auth.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRegisterInitResponse {
    private Long kakaoId;
    private String kakaoName;
}