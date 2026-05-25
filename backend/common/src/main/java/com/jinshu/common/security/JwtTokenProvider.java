package com.jinshu.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String JWT_SECRET = System.getenv("JINSHU_JWT_SECRET");
    private static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private final SecretKey secretKey;

    public JwtTokenProvider() {
        String secret = JWT_SECRET != null ? JWT_SECRET : "defaultSecretKeyForDevelopmentOnlyChangeInProduction12345678!xyz";
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtTokenProvider(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, Long tenantId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_id", tenantId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String createRefreshToken(Long userId, Long tenantId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_id", tenantId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
            return false;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public Long getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("tenant_id", Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public String getTokenIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    public String getTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    public Long getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime();
    }

    public long getAccessTokenExpiration() {
        return ACCESS_TOKEN_EXPIRATION;
    }

    public long getRefreshTokenExpiration() {
        return REFRESH_TOKEN_EXPIRATION;
    }
}
