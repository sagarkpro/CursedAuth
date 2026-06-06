package com.cursed.auth.repository;

import com.cursed.auth.entities.RefreshToken;

public interface RefreshTokenRepositoryCustom {

    /**
     * Atomically claims a refresh token for rotation: succeeds only if the token is not
     * already revoked and not already replaced. Sets {@code replacedByHash} and revokes
     * the old token, returning its pre-image. Returns {@code null} if it could not be
     * claimed (already rotated/revoked concurrently) — the caller should treat that as
     * token reuse.
     */
    RefreshToken claimForRotation(String tokenHash, String replacedByHash);

    /** Revokes every refresh token in a rotation family (reuse-detection response). */
    void revokeFamily(String familyId);
}
