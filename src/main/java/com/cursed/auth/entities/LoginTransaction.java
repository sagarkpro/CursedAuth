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
 * A short-lived OAuth authorization request captured at GET /authorize, before the
 * user has logged in. The id is the {@code login_id} handed to the login UI; the
 * login API resolves it to mint an authorization code. Auto-expires via TTL index.
 */
@Document(collection = "oauth_login_transactions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginTransaction {
    @Id
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

    @Indexed(expireAfter = "10m")
    Instant createdAt;
}
