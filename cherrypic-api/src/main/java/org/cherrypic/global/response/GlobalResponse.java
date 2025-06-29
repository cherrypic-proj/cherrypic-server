package org.cherrypic.global.response;

import java.time.LocalDateTime;

public record GlobalResponse<T>(boolean success, int status, LocalDateTime timestamp, T data) {
    public static <T> GlobalResponse<T> success(int status, T data) {
        return new GlobalResponse<>(true, status, LocalDateTime.now(), data);
    }

    public static <T> GlobalResponse<T> fail(int status, T errorResponse) {
        return new GlobalResponse<>(false, status, LocalDateTime.now(), errorResponse);
    }
}
