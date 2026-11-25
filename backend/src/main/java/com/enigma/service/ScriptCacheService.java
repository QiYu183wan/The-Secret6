package com.enigma.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 副本缓存。Redis 不可用时自动降级为数据库读取，30 秒后重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptCacheService {

    public static final String PUBLISHED_LIST_KEY = "enigma:script:published";
    private static final String DETAIL_KEY_PREFIX = "enigma:script:detail:";
    private static final long RETRY_INTERVAL_MILLIS = 30_000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile long retryAfter = 0L;
    private volatile boolean warned = false;

    public <T> T get(String key, TypeReference<T> type) {
        if (unavailable()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            markUnavailable(e);
            return null;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (unavailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            markUnavailable(e);
        }
    }

    public void evictScript(Long scriptId) {
        try {
            redisTemplate.delete(PUBLISHED_LIST_KEY);
            if (scriptId != null) {
                redisTemplate.delete(DETAIL_KEY_PREFIX + scriptId);
            }
        } catch (Exception e) {
            markUnavailable(e);
        }
    }

    public String detailKey(Long scriptId) {
        return DETAIL_KEY_PREFIX + scriptId;
    }

    private boolean unavailable() {
        return System.currentTimeMillis() < retryAfter;
    }

    private void markUnavailable(Exception e) {
        retryAfter = System.currentTimeMillis() + RETRY_INTERVAL_MILLIS;
        if (!warned) {
            warned = true;
            log.warn("Redis 不可用，副本缓存已降级为数据库读取：{}", e.getMessage());
        }
    }
}
