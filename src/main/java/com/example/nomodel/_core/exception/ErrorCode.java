package com.example.nomodel._core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("ISE001", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    ;

    private final String errorCode;
    private final HttpStatus status;
    private final String message;
    
    ErrorCode(String errorCode, HttpStatus status, String message) {
        this.errorCode = errorCode;
        this.status = status;
        this.message = message;
    }
}
