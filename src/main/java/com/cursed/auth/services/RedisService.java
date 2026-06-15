package com.cursed.auth.services;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

@Service
public class RedisService {

    private final RedisTemplate<String, JsonNode> redisTemplate;

    public RedisService(RedisTemplate<String, JsonNode> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, JsonNode json, long minutes) {
        redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(minutes));
    }

    public void save(String key, JsonNode json) {
        redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10));
    }

    public void save(String key, String value, long minutes) {
        redisTemplate.opsForValue().set(key, StringNode.valueOf(value), Duration.ofMinutes(minutes));
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, StringNode.valueOf(value), Duration.ofMinutes(10));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public JsonNode getJson(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public JsonNode consumeOnce(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    public String getString(String key) {
        JsonNode res = redisTemplate.opsForValue().get(key);
        if (res != null && !res.isNull() && res.isString()) {
            return res.asString();
        }
        return null;
    }

    public String consumeOnceString(String key) {
        JsonNode res = redisTemplate.opsForValue().getAndDelete(key);
        if (res != null && !res.isNull() && res.isString()) {
            return res.asString();
        }
        return null;
    }
}
