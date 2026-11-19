package com.wjys.support.cache;

import java.time.Duration;
import java.util.Optional;

public interface DashboardCacheService {
    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    void evictByPrefix(String prefix);
}
