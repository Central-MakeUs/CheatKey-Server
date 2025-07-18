package com.cheatkey.common.exception;

import lombok.Getter;

import java.io.IOException;

@Getter
public class ImageException extends IOException {

    private ErrorCode errorCode;

    public ImageException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
