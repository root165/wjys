package com.wjys.security;

import java.time.Instant;

public interface TokenBlacklistService {
    void revoke(String jti, Instant expiresAt);

    boolean isRevoked(String jti);
}
