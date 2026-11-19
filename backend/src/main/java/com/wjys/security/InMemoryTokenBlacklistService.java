package com.wjys.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "wjys.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenBlacklistService implements TokenBlacklistService {
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {
        revokedTokens.put(jti, expiresAt);
    }

    @Override
    public boolean isRevoked(String jti) {
        Instant expiresAt = revokedTokens.get(jti);
        if (expiresAt == null) return false;
        if (expiresAt.isBefore(Instant.now())) {
            revokedTokens.remove(jti);
            return false;
        }
        return true;
    }
}
