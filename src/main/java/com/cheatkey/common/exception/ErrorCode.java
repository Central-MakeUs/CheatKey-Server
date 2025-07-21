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
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 요청 실패"),

    AUTH_INVALID_NICKNAME_LENGTH(HttpStatus.BAD_REQUEST, "닉네임은 2~5자여야 합니다."),
    AUTH_INVALID_NICKNAME_EMOJI(HttpStatus.BAD_REQUEST, "닉네임에 이모지는 사용할 수 없습니다."),
    AUTH_DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    AUTH_NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "최소 2글자 이상 입력해주세요"),
    AUTH_MISSING_REQUIRED_INFORMATION(HttpStatus.BAD_REQUEST, "필수 정보가 누락되었습니다."),
    AUTH_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 사용자입니다."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),

    AUTH_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾지 못했습니다. 다시 로그인 해주세요."),

    AUTH_REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 가입할 수 있습니다."),

    // Detection (피싱 검색)
    INVALID_INPUT_TYPE_URL(HttpStatus.BAD_REQUEST, "검사 입력 타입은 URL이어야 합니다."),
    INVALID_INPUT_TYPE_CASE(HttpStatus.BAD_REQUEST, "검사 입력 타입은 CASE이어야 합니다."),
    DETECTION_FAILED(HttpStatus.BAD_REQUEST, "피싱 분석 중 오류가 발생 했습니다. 다시 시도해 주세요."),
    GOOGLE_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Google Safe Browsing API 호출에 실패했습니다. 나중에 다시 시도해주세요."),

    // JWT
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "헤더에 Access Token을 찾을 수 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료 되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.BAD_REQUEST, "토큰 형식에 문제가 있습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Refresh Token을 찾을 수 없습니다."),

    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 provider입니다."),

    // Cookie
    COOKIE_NOT_FOUND(HttpStatus.BAD_REQUEST, "요청에 쿠키가 존재하지 않습니다."),

    // 파일 처리
    FILE_EXTENSION_FAULT(HttpStatus.BAD_REQUEST, "해당 파일 확장자 명이 존재하지 않습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Presigned URL 생성에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FILE_NOT_PERMANENT(HttpStatus.BAD_REQUEST, "임시 파일은 영구 파일로만 접근할 수 있습니다."),


    /**
     * 시스템 예외
     */
    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // 500 Internal Server Error,
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB 처리 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
