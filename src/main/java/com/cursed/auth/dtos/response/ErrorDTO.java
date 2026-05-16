package com.cursed.auth.dtos.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ErrorDTO {
    String code;
    String message;
    String details;
    LocalDateTime timestamp;
    HttpStatus status;
}
