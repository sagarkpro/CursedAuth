package com.cursed.auth.records;

public record RedisOTPVerification(
        String otp,
        String email) {
}
