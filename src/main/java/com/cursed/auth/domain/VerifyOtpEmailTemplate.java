package com.cursed.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpEmailTemplate {
    private String receiverName;
    private String otp;
    private String ssoBasePath;
    private int tmYear;
}
