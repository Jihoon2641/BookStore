package com.bookstore.bookstore_api.util.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    private final T data;
    private final String message;
    private final HttpStatus statusCode;

    public static <T> ApiResponse<T> success(T data, HttpStatus statusCode) {
        return new ApiResponse<>(data, "Success", statusCode);
    }

    public static <T> ApiResponse<T> success(T data, String message, HttpStatus statusCode) {
        return new ApiResponse<>(data, message, statusCode);
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus statusCode) {
        return new ApiResponse<>(null, message, statusCode);
    }
}
