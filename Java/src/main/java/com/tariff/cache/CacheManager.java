package com.tariff.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tariff.connection.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private final ConnectionPool connectionPool;
    private final ObjectMapper objectMapper;

    public CacheManager(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public String generateCacheKey(String prefix, String functionName, Object... args) {
        try {
            String keyData = functionName + ":" + Arrays.toString(args);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(keyData.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return prefix + ":" + sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.warn("MD5 not available, using simple key generation");
            return prefix + ":" + functionName + ":" + Arrays.hashCode(args);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try (Jedis jedis = connectionPool.getJedisPool().getResource()) {
            String cached = jedis.get(key);
            if (cached != null) {
                logger.debug("Cache hit for key: {}", key);
                return objectMapper.readValue(cached, clazz);
            }
        } catch (Exception e) {
            logger.warn("Cache read error for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    public void set(String key, Object value, int ttlSeconds) {
        try (Jedis jedis = connectionPool.getJedisPool().getResource()) {
            String json = objectMapper.writeValueAsString(value);
            jedis.setex(key, ttlSeconds, json);
            logger.debug("Cached result for key: {}", key);
        } catch (JsonProcessingException e) {
            logger.warn("Cache write error for key {}: {}", key, e.getMessage());
        }
    }
}