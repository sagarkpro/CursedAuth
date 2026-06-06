package com.cursed.auth.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PkceUtils {

    private PkceUtils() {
    }

    /**
     * Verifies an RFC 7636 S256 PKCE challenge: base64url(SHA-256(code_verifier)) must
     * equal the stored code_challenge. Constant-time comparison. OAuth 2.1 only allows
     * the S256 method (plain is rejected at /authorize).
     */
    public static boolean verifyS256(String codeVerifier, String storedChallenge) {
        if (codeVerifier == null || storedChallenge == null) {
            return false;
        }
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
        String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                storedChallenge.getBytes(StandardCharsets.US_ASCII));
    }
}
