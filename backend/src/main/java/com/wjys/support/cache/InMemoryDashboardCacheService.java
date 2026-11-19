package com.wjys.support.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "wjys.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryDashboardCacheService implements DashboardCacheService {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return Optional.empty();
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        entries.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    @Override
    public void evictByPrefix(String prefix) {
        entries.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private record Entry(String value, Instant expiresAt) { }
}
