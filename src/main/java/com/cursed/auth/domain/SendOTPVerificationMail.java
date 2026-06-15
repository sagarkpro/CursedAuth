package com.cursed.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendOTPVerificationMail {
    String receiverEmail;
    String receiverName;
    String OTP;
}
