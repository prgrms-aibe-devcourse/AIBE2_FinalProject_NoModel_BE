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
    MEMBER_NOT_ACTIVE("MNA001", HttpStatus.FORBIDDEN, "Member is not active"),
    
    // 인증 관련 에러
    INVALID_PASSWORD("IP001", HttpStatus.UNAUTHORIZED, "Invalid password"),
    
    // JWT 관련 에러
    INVALID_JWT_TOKEN("IJT001", HttpStatus.BAD_REQUEST, "Invalid JWT token"),
    EMPTY_JWT_CLAIMS("EJC001", HttpStatus.BAD_REQUEST, "JWT claims is empty"),
    AUTHENTICATION_FAILED("AF001", HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED("AD001", HttpStatus.FORBIDDEN, "Access denied"),
    INVALID_JWT_SIGNATURE("IJS001", HttpStatus.BAD_REQUEST, "Invalid JWT signature"),
    NO_AUTHORITIES_IN_TOKEN("NAT001", HttpStatus.FORBIDDEN, "No authorities in token"),
    TOKEN_NOT_FOUND("TNF001", HttpStatus.UNAUTHORIZED, "Token not found in request header"),
    INVALID_TOKEN("IT001", HttpStatus.UNAUTHORIZED, "Invalid token"),
    INVALID_REFRESH_TOKEN("IRT001", HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    INVALID_TOKEN_TYPE("ITT001", HttpStatus.BAD_REQUEST, "Invalid token type"),
    REFRESH_TOKEN_NOT_FOUND("RTNF001", HttpStatus.NOT_FOUND, "Refresh token not found"),
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
