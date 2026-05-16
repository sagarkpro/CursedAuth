package com.cursed.auth.services;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class RedisService {

    private final RedisTemplate<String, JsonNode> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, JsonNode> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(String key, JsonNode json, long minutes) {
        redisTemplate.opsForValue().set(key, json, minutes);
    }

    public void save(String key, JsonNode json) {
        redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10));
    }

    public void save(String key, String value, long minutes) {
        redisTemplate.opsForValue().set(key, objectMapper.valueToTree(value), minutes);
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, objectMapper.valueToTree(value), Duration.ofMinutes(10));
    }

    public JsonNode getJson(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public String getString(String key) {
        JsonNode res = redisTemplate.opsForValue().get(key);
        if (res != null && !res.isEmpty() && res.isString()) {
            return res.asString();
        }
        return null;
    }
}
