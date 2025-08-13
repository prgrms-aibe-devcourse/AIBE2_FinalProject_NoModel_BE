package com.example.nomodel._core.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final LocalDateTime timestamp;

    public ApplicationException(ErrorCode errorCode){
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
}
