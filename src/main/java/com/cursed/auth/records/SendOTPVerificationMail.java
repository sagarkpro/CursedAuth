package com.cursed.auth.records;

public record SendOTPVerificationMail(
        String receiverEmail,
        String receiverName,
        String OTP) {
}
