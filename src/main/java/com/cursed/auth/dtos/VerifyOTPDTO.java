package com.cursed.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOTPDTO {
    @NotBlank
    @Email
    String email;

    @NotBlank
    @Size(min = 6, max = 6)
    String otp;
}
