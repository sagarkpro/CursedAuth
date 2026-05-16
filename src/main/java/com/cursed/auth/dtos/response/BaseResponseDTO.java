package com.cursed.auth.dtos.response;

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
public class BaseResponseDTO<T> {
    boolean success;

    @Builder.Default
    ErrorDTO error = null;

    T data;
}
