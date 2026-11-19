package com.wjys.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final SecretKey secretKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(
            @Value("${wjys.security.jwt-secret}") String secret,
            @Value("${wjys.security.access-token-minutes:30}") long accessMinutes,
            @Value("${wjys.security.refresh-token-days:7}") long refreshDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    public IssuedToken issueAccess(WjysPrincipal principal) {
        return issue(principal, "access", accessTtl);
    }

    public IssuedToken issueRefresh(WjysPrincipal principal) {
        return issue(principal, "refresh", refreshTtl);
    }

    public TokenClaims parse(String token) {
        try {
            Jws<Claims> parsed = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            Claims claims = parsed.getBody();
            return new TokenClaims(
                    claims.getId(),
                    claims.getSubject(),
                    claims.get("token_type", String.class),
                    claims.get("token_version", Integer.class),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Token 无效或已过期", exception);
        }
    }

    public Duration getAccessTtl() {
        return accessTtl;
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    private IssuedToken issue(WjysPrincipal principal, String type, Duration ttl) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setId(jti)
                .setSubject(principal.username())
                .claim("user_id", principal.id())
                .claim("token_type", type)
                .claim("token_version", principal.tokenVersion())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        return new IssuedToken(token, jti, now.plus(ttl));
    }

    public record IssuedToken(String value, String jti, Instant expiresAt) {
    }

    public record TokenClaims(String jti, String username, String type, Integer tokenVersion, Instant expiresAt) {
    }
}
