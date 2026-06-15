package com.cursed.auth.constants;

public final class RedisKeys {
    private RedisKeys() {
        super();
    }

    public static final String OTP_VERIFICATION = "_otpVerification";
    public static final String PASSWORD_RESET = "_passwordReset";
    public static final String USER_PROFILE_IMAGE = "_profileImage";
}
