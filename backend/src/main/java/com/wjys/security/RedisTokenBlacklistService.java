package com.wjys.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wjys.redis.enabled", havingValue = "true")
public class RedisTokenBlacklistService implements TokenBlacklistService {
    private static final String KEY_PREFIX = "wjys:auth:blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }

    @Override
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
