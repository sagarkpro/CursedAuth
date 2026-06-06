package com.cursed.auth.entities;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single-use OAuth 2.1 authorization code bound to a user, client, redirect URI and
 * the PKCE challenge captured at /authorize. The id IS the code value. Consumption is
 * atomic (see {@code AuthorizationCodeRepository.consumeOnce}); also auto-expires via TTL.
 */
@Document(collection = "oauth_authorization_codes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationCode {
    @Id
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

    @Builder.Default
    boolean consumed = false;

    @Indexed(expireAfter = "60s")
    Instant createdAt;
}
