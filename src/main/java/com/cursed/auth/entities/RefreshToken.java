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
 * A persisted refresh token. Only the SHA-256 hash of the opaque token is stored.
 * Tokens rotate by default: on use, the old doc gets {@code replacedByHash} set and is
 * revoked, and a new doc is created with the same {@code familyId}. Presenting a token
 * that is already revoked/replaced is treated as reuse and revokes the whole family.
 * Auto-expires via a per-document TTL on {@code expiresAt}.
 */
@Document(collection = "oauth_refresh_tokens")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    @Id
    String id;

    @Indexed
    String tokenHash;

    String clientId;
    String userId;
    String audience;
    Set<String> scopes;

    String familyId;
    String replacedByHash;

    @Builder.Default
    boolean revoked = false;

    Instant issuedAt;

    @Indexed(expireAfter = "0s")
    Instant expiresAt;
}
