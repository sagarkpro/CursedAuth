package com.cursed.auth.utils;

import org.springframework.http.ResponseEntity;

import com.cursed.auth.dtos.response.BaseResponseDTO;

public class CommonUtils {
    private CommonUtils() {
    }

    public static <T> ResponseEntity<BaseResponseDTO<T>> handleResponse(BaseResponseDTO<T> response) {
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        if (response.getError() != null && response.getError().getStatus() != null) {
            return ResponseEntity.status(response.getError().getStatus()).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
