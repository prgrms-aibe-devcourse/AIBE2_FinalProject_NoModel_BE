package com.example.nomodel._core.exception;

import com.example.nomodel._core.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        LocalDateTime timestamp
    ) {}

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<?> applicationHandler(ApplicationException e){
        log.error("Error occurs {}", e.toString());

        ErrorResponse errorResponse = new ErrorResponse(
            e.getErrorCode().getStatus().value(),
            e.getErrorCode().getErrorCode(),
            e.getErrorCode().getMessage(),
            e.getTimestamp()
        );

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ApiUtils.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> applicationHandler(Exception e){
        log.error("Error occurs {}", e.getMessage());

        ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse errorResponse = new ErrorResponse(
            error.getStatus().value(),
            error.getErrorCode(),
            e.getMessage(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiUtils.error(errorResponse));
    }
}
