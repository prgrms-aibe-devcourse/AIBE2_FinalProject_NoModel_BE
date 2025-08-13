package com.example.nomodel._core.utils;

import org.springframework.http.HttpStatus;

public class ApiUtils {

    public static <T> ApiResult<T> success(T response) {
        return new ApiResult<>(true, response, null);
    }

    public static ApiResult<?> error(String message, HttpStatus status) {
        return new ApiResult<>(false, null, new ApiError(message, status.value()));
    }

    public static <T> ApiResult<T> error(T data) {
        return new ApiResult<>(false, null, data);
    }

    public record ApiResult<T>(
            boolean success,
            T response,
            T error
    ) {}

    public record ApiError(
            String message,
            int status
    ) {}
}
