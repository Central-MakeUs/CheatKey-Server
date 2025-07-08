package com.cheatkey.module.auth.interfaces.dto.login;

public record LoginUserDto(
        Long kakaoId,
        String nickname
) {}

