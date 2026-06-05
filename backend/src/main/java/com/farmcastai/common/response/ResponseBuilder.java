package com.farmcastai.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseBuilder {
    private ResponseBuilder() {
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(T data) {
        return ResponseEntity.ok(BaseResponse.success("Success", data));
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(BaseResponse.success(message, data));
    }

    public static <T> ResponseEntity<BaseResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(message, data));
    }

    public static <T> ResponseEntity<BaseResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(BaseResponse.error(status, message));
    }

    public static <T> ResponseEntity<BaseResponse<T>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    public static <T> ResponseEntity<BaseResponse<T>> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }
}
