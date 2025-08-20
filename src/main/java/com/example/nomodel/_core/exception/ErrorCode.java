package com.example.nomodel._core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("ISE001", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    INVALID_REQUEST("IRE001", HttpStatus.BAD_REQUEST, "Invalid request"),
    MEMBER_NOT_FOUND("MNF001", HttpStatus.NOT_FOUND, "Member not found"),
    MEMBER_ALREADY_EXISTS("MAE001", HttpStatus.CONFLICT, "Member already exists"),
    EMAIL_ALREADY_EXISTS("EAE001", HttpStatus.CONFLICT, "Email already exists"),
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
