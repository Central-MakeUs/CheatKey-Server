package com.cheatkey.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /**
     * 비즈니스 예외
     */

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Auth (회원)
    AUTH_INVALID_NICKNAME_LENGTH(HttpStatus.BAD_REQUEST, "닉네임은 2~5자여야 합니다."),
    AUTH_INVALID_NICKNAME_EMOJI(HttpStatus.BAD_REQUEST, "닉네임에 이모지는 사용할 수 없습니다."),
    AUTH_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    AUTH_NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "최소 2글자 이상 입력해주세요"),
    AUTH_MISSING_REQUIRED_INFORMATION(HttpStatus.BAD_REQUEST, "필수 정보가 누락되었습니다."),
    AUTH_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다."),
    AUTH_UNAUTHORIZED(HttpStatus.BAD_REQUEST, "잘못된 회원가입 접근입니다. 로그인을 다시 해주세요."),

    /**
     * 시스템 예외
     */

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // 500 Internal Server Error
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB 처리 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
