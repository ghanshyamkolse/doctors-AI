package com.scribe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        String status,
        String message,
        T data,
        Instant timestamp
) {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(STATUS_SUCCESS, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(STATUS_ERROR, message, data, Instant.now());
    }
}
