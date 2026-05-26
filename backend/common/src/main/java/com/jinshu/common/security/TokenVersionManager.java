package com.jinshu.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenVersionManager {

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_VERSION_KEY = "user:%d:token_version";
    private static final String TOKEN_BLACKLIST_KEY = "token_blacklist:%s";
    private static final String REFRESH_TOKEN_KEY = "refresh:%d:%s";
    private static final String LOGIN_FAIL_KEY = "login_fail:%s";

    public boolean isTokenValid(Long userId, Integer tokenVersion) {
        try {
            String currentVersion = redisTemplate.opsForValue().get(String.format(TOKEN_VERSION_KEY, userId));
            if (currentVersion == null) {
                return true;
            }
            return Integer.parseInt(currentVersion) <= tokenVersion;
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping version check: {}", e.getMessage());
            return true;
        }
    }

    public long getCurrentVersion(Long userId) {
        try {
            String v = redisTemplate.opsForValue().get(String.format(TOKEN_VERSION_KEY, userId));
            return v != null ? Long.parseLong(v) : 0;
        } catch (Exception e) {
            log.warn("Redis unavailable, returning default version: {}", e.getMessage());
            return 0;
        }
    }

    public void incrementTokenVersion(Long userId) {
        try {
            redisTemplate.opsForValue().increment(String.format(TOKEN_VERSION_KEY, userId));
        } catch (Exception e) {
            log.warn("Failed to increment token version: {}", e.getMessage());
        }
    }

    public void addToBlacklist(String jti, long remainingExpiration) {
        try {
            if (remainingExpiration > 0) {
                redisTemplate.opsForValue().set(
                        String.format(TOKEN_BLACKLIST_KEY, jti),
                        "1",
                        remainingExpiration,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("Failed to add token to blacklist: {}", e.getMessage());
        }
    }

    public boolean isInBlacklist(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(String.format(TOKEN_BLACKLIST_KEY, jti)));
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping blacklist check: {}", e.getMessage());
            return false;
        }
    }

    public void storeRefreshToken(Long userId, String tokenId, long expiration) {
        try {
            redisTemplate.opsForValue().set(
                    String.format(REFRESH_TOKEN_KEY, userId, tokenId),
                    "1",
                    expiration,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.warn("Failed to store refresh token: {}", e.getMessage());
        }
    }

    public boolean isRefreshTokenValid(Long userId, String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(String.format(REFRESH_TOKEN_KEY, userId, tokenId)));
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping refresh token check: {}", e.getMessage());
            return true;
        }
    }

    public void deleteRefreshToken(Long userId, String tokenId) {
        try {
            redisTemplate.delete(String.format(REFRESH_TOKEN_KEY, userId, tokenId));
        } catch (Exception e) {
            log.warn("Failed to delete refresh token: {}", e.getMessage());
        }
    }

    public void incrementLoginFailCount(String username) {
        try {
            String key = String.format(LOGIN_FAIL_KEY, username);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, 15, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("Failed to increment login fail count: {}", e.getMessage());
        }
    }

    public void clearLoginFailCount(String username) {
        try {
            redisTemplate.delete(String.format(LOGIN_FAIL_KEY, username));
        } catch (Exception e) {
            log.warn("Failed to clear login fail count: {}", e.getMessage());
        }
    }

    public long getLoginFailCount(String username) {
        try {
            String count = redisTemplate.opsForValue().get(String.format(LOGIN_FAIL_KEY, username));
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            log.warn("Failed to get login fail count: {}", e.getMessage());
            return 0;
        }
    }

    public long getLoginFailExpireTime(String username) {
        try {
            Long expire = redisTemplate.getExpire(String.format(LOGIN_FAIL_KEY, username), TimeUnit.SECONDS);
            return expire != null ? expire : 0;
        } catch (Exception e) {
            log.warn("Failed to get login fail expire time: {}", e.getMessage());
            return 0;
        }
    }
}
