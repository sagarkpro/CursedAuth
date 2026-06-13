package com.cursed.auth.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.LoginTransaction;

import tools.jackson.databind.ObjectMapper;

/**
 * Redis-backed store for short-lived OAuth login transactions. Transactions live under
 * {@code logintx:{id}} with a 10m TTL and are deleted once an authorization code is minted.
 */
@Repository
public class LoginTransactionRepository {

    private static final String KEY_PREFIX = "logintx:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public LoginTransactionRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(LoginTransaction tx) {
        redis.opsForValue().set(KEY_PREFIX + tx.getId(), objectMapper.writeValueAsString(tx), TTL);
    }

    public Optional<LoginTransaction> findById(String id) {
        String json = redis.opsForValue().get(KEY_PREFIX + id);
        return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, LoginTransaction.class));
    }

    public void deleteById(String id) {
        redis.delete(KEY_PREFIX + id);
    }
}
