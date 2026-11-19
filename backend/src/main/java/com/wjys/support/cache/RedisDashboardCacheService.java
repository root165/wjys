package com.wjys.support.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wjys.redis.enabled", havingValue = "true")
public class RedisDashboardCacheService implements DashboardCacheService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> get(String key) {
        try { return Optional.ofNullable(redisTemplate.opsForValue().get(key)); } catch (RuntimeException ignored) { return Optional.empty(); }
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        try { redisTemplate.opsForValue().set(key, value, ttl); } catch (RuntimeException ignored) { }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (RuntimeException ignored) { }
    }
}
