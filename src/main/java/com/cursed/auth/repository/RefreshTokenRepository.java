package com.cursed.auth.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.RefreshToken;

import tools.jackson.databind.ObjectMapper;

/**
 * Redis-backed store for refresh tokens, keyed by SHA-256 hash under {@code refresh:{tokenHash}}.
 * Each token also joins a {@code refreshfam:{familyId}} set so the whole family can be revoked
 * on reuse detection. Tokens auto-expire via a per-key TTL derived from {@code expiresAt}.
 *
 * <p>Rotation is made atomic with a {@code refreshclaim:{tokenHash}} marker set via SETNX: only
 * the first caller to claim a token may rotate it; concurrent callers lose the race and get
 * {@code null}, which the caller treats as reuse.
 */
@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";
    private static final String FAMILY_PREFIX = "refreshfam:";
    private static final String CLAIM_PREFIX = "refreshclaim:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RefreshTokenRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(RefreshToken token) {
        Duration ttl = ttlFor(token);
        redis.opsForValue().set(KEY_PREFIX + token.getTokenHash(), objectMapper.writeValueAsString(token), ttl);
        String familyKey = FAMILY_PREFIX + token.getFamilyId();
        redis.opsForSet().add(familyKey, token.getTokenHash());
        redis.expire(familyKey, ttl);
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        String json = redis.opsForValue().get(KEY_PREFIX + tokenHash);
        return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, RefreshToken.class));
    }

    /**
     * Atomically claim the token for rotation. Returns the claimed token on success, or
     * {@code null} if the claim was already taken (concurrent rotation) or the token is gone.
     * On success the stored record is marked revoked with {@code replacedByHash} set, so a
     * later replay is detected as reuse.
     */
    public RefreshToken claimForRotation(String tokenHash, String replacedByHash) {
        RefreshToken existing = findByTokenHash(tokenHash).orElse(null);
        if (existing == null || existing.isRevoked() || existing.getReplacedByHash() != null) {
            return null;
        }
        Duration ttl = ttlFor(existing);
        Boolean won = redis.opsForValue().setIfAbsent(CLAIM_PREFIX + tokenHash, replacedByHash, ttl);
        if (!Boolean.TRUE.equals(won)) {
            return null; // lost the race -> concurrent rotation == reuse
        }
        existing.setReplacedByHash(replacedByHash);
        existing.setRevoked(true);
        redis.opsForValue().set(KEY_PREFIX + tokenHash, objectMapper.writeValueAsString(existing), ttl);
        return existing;
    }

    public void revokeFamily(String familyId) {
        String familyKey = FAMILY_PREFIX + familyId;
        Set<String> hashes = redis.opsForSet().members(familyKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redis.delete(KEY_PREFIX + hash);
                redis.delete(CLAIM_PREFIX + hash);
            }
        }
        redis.delete(familyKey);
    }

    private static Duration ttlFor(RefreshToken token) {
        if (token.getExpiresAt() == null) {
            return Duration.ofSeconds(1);
        }
        Duration ttl = Duration.between(Instant.now(), token.getExpiresAt());
        return ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
    }
}
