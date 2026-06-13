package com.cursed.auth.entities;

import java.time.Instant;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A short-lived OAuth authorization request captured at GET /authorize, before the
 * user has logged in. The {@code id} is the {@code login_id} handed to the login UI;
 * the login API resolves it to mint an authorization code. Stored in Redis under
 * {@code logintx:{id}} and auto-expires via Redis TTL.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginTransaction {
    String id;

    String clientId;
    String redirectUri;
    Set<String> scopes;
    String state;
    String nonce;
    String codeChallenge;
    String codeChallengeMethod;
    String audience;
    String responseType;

    Instant createdAt;
}
