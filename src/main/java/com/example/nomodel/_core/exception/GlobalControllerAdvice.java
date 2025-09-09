package com.example.nomodel._core.exception;

import com.example.nomodel._core.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        log.error("ApplicationException occurs: ", e);

        ErrorResponse errorResponse = new ErrorResponse(
            e.getErrorCode().getStatus().value(),
            e.getErrorCode().getErrorCode(),
            e.getErrorCode().getMessage(),
            e.getTimestamp()
        );

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ApiUtils.error(errorResponse));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationHandler(MethodArgumentNotValidException e) {
        log.error("Validation error occurs: ", e);

        // 첫 번째 필드 에러의 메시지를 사용
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.INVALID_REQUEST.getErrorCode(),
            errorMessage,
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiUtils.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> applicationHandler(Exception e){
        log.error("Unhandled Exception occurs: ", e);

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
