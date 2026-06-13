package com.cursed.auth.entities;

import java.time.Instant;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A persisted refresh token. Only the SHA-256 hash of the opaque token is stored.
 * Tokens rotate by default: on use, the old record gets {@code replacedByHash} set and
 * is revoked, and a new record is created with the same {@code familyId}. Presenting a
 * token that is already revoked/replaced is treated as reuse and revokes the whole family.
 * Stored in Redis under {@code refresh:{tokenHash}} (with a {@code refreshfam:{familyId}}
 * set for family revocation); auto-expires via a per-key Redis TTL derived from {@code expiresAt}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    String id;

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

    Instant expiresAt;
}
