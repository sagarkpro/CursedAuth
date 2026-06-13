package com.cursed.auth.entities;

import java.time.Instant;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single-use OAuth 2.1 authorization code bound to a user, client, redirect URI and
 * the PKCE challenge captured at /authorize. The {@code code} IS the value. Stored in
 * Redis under {@code authcode:{code}}; consumption is atomic (single-use GETDEL, see
 * {@code AuthorizationCodeRepository.consumeOnce}) and the key auto-expires via Redis TTL.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationCode {
    String code;

    String clientId;
    String userId;
    String redirectUri;
    Set<String> scopes;
    String nonce;
    String codeChallenge;
    String codeChallengeMethod;
    String audience;
    Instant authTime;

    Instant createdAt;
}
