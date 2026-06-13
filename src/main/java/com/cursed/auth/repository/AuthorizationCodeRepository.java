package com.cursed.auth.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.AuthorizationCode;

import tools.jackson.databind.ObjectMapper;

/**
 * Redis-backed store for single-use OAuth authorization codes. Codes live under
 * {@code authcode:{code}} with a 60s TTL. Single-use is enforced atomically by GETDEL:
 * the first {@code consumeOnce} reads and deletes the key in one step, so any later
 * attempt finds nothing.
 */
@Repository
public class AuthorizationCodeRepository {

    private static final String KEY_PREFIX = "authcode:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AuthorizationCodeRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(AuthorizationCode code) {
        redis.opsForValue().set(KEY_PREFIX + code.getCode(), objectMapper.writeValueAsString(code), TTL);
    }

    /**
     * Atomically read and delete the code. Returns the pre-image on the first call, or
     * {@code null} if the code is unknown, already used, or expired.
     */
    public AuthorizationCode consumeOnce(String code) {
        String json = redis.opsForValue().getAndDelete(KEY_PREFIX + code);
        return json == null ? null : objectMapper.readValue(json, AuthorizationCode.class);
    }
}
