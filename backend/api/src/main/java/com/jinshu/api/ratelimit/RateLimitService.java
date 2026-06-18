package com.jinshu.api.ratelimit;

import com.jinshu.api.config.RateLimitProperties;
import com.jinshu.api.dao.RateLimitConfigMapper;
import com.jinshu.api.dao.TenantConcurrencyQuotaMapper;
import com.jinshu.common.entity.RateLimitConfig;
import com.jinshu.common.entity.TenantConcurrencyQuota;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * API 限流与租户并发配额服务
 *
 * 基于 Redis 实现滑动窗口限流与租户级并发计数：
 * - 滑动窗口：使用 Redis Sorted Set，以请求时间戳为 score，窗口过期后自动清理旧记录。
 * - 并发配额：使用 Redis 计数器，请求进入时 INCR，完成时 DECR。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfigMapper rateLimitConfigMapper;
    private final TenantConcurrencyQuotaMapper concurrencyQuotaMapper;
    private final RateLimitProperties properties;

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local now = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            local minScore = now - window
            redis.call('ZREMRANGEBYSCORE', key, 0, minScore)
            local current = redis.call('ZCARD', key)
            if current < limit then
                redis.call('ZADD', key, now, member)
                redis.call('EXPIRE', key, math.ceil(window / 1000))
                return current + 1
            else
                return current
            end
            """, Long.class);

    private static final DefaultRedisScript<Long> CONCURRENCY_ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local max = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or '0')
            if current >= max then
                return -1
            end
            local new = redis.call('INCR', key)
            redis.call('EXPIRE', key, ttl)
            return new
            """, Long.class);

    private static final DefaultRedisScript<Long> CONCURRENCY_RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local current = tonumber(redis.call('GET', key) or '0')
            if current > 0 then
                redis.call('DECR', key)
                return current - 1
            end
            return 0
            """, Long.class);

    /**
     * 检查当前请求是否通过滑动窗口限流
     *
     * @param scope     限流维度
     * @param tenantId  租户 ID（租户级限流使用）
     * @param userId    用户 ID（用户级限流使用）
     * @param clientIp  客户端 IP（登录 IP 级限流使用）
     * @param path      请求路径
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean checkRateLimit(RateLimitScope scope, Long tenantId, Long userId, String clientIp, String path) {
        RateLimitConfig config = resolveConfig(scope, tenantId);
        if (config == null || Boolean.FALSE.equals(config.getEnabled())
                || config.getMaxRequests() == null || config.getWindowSeconds() == null) {
            return true;
        }

        String key = buildRateLimitKey(scope, tenantId, userId, clientIp, path);
        long windowMillis = config.getWindowSeconds() * 1000L;
        long now = System.currentTimeMillis();
        String member = now + ":" + Thread.currentThread().threadId() + ":" + Math.abs(key.hashCode());

        Long count = redisTemplate.execute(SLIDING_WINDOW_SCRIPT, Collections.singletonList(key),
                String.valueOf(windowMillis), String.valueOf(now),
                String.valueOf(config.getMaxRequests()), member);

        if (count == null) {
            log.warn("Redis 限流脚本返回空，放行请求");
            return true;
        }
        return count <= config.getMaxRequests();
    }

    /**
     * 尝试获取租户并发请求配额
     *
     * @param tenantId 租户 ID
     * @return true 表示获取成功，false 表示并发已满
     */
    public boolean acquireTenantConcurrency(Long tenantId) {
        if (tenantId == null) {
            return true;
        }

        TenantConcurrencyQuota quota = concurrencyQuotaMapper.selectByTenantId(tenantId);
        int max = quota != null && quota.getMaxConcurrent() != null && Boolean.TRUE.equals(quota.getEnabled())
                ? quota.getMaxConcurrent() : properties.getDefaultTenantConcurrency();

        String key = buildConcurrencyKey(tenantId);
        Long result = redisTemplate.execute(CONCURRENCY_ACQUIRE_SCRIPT, Collections.singletonList(key),
                String.valueOf(max), String.valueOf(properties.getConcurrencyCounterTtlSeconds()));

        if (result == null) {
            log.warn("Redis 并发配额脚本返回空，放行请求");
            return true;
        }
        return result >= 0;
    }

    /**
     * 释放租户并发请求配额
     *
     * @param tenantId 租户 ID
     */
    public void releaseTenantConcurrency(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        String key = buildConcurrencyKey(tenantId);
        redisTemplate.execute(CONCURRENCY_RELEASE_SCRIPT, Collections.singletonList(key));
    }

    private RateLimitConfig resolveConfig(RateLimitScope scope, Long tenantId) {
        RateLimitConfig config = null;
        if (scope == RateLimitScope.TENANT && tenantId != null) {
            config = rateLimitConfigMapper.selectByScopeAndTenant(scope.name(), tenantId);
        }
        if (config == null) {
            config = rateLimitConfigMapper.selectGlobalByScope(scope.name());
        }
        if (config == null) {
            config = createDefaultConfig(scope);
        }
        return config;
    }

    private RateLimitConfig createDefaultConfig(RateLimitScope scope) {
        RateLimitConfig config = new RateLimitConfig();
        config.setScope(scope.name());
        config.setResourcePattern("*");
        config.setEnabled(true);
        switch (scope) {
            case USER -> {
                config.setMaxRequests(properties.getDefaultUserQps());
                config.setWindowSeconds(1);
            }
            case TENANT -> {
                config.setMaxRequests(properties.getDefaultTenantQps());
                config.setWindowSeconds(1);
            }
            case LOGIN_IP -> {
                config.setMaxRequests(properties.getDefaultLoginPerMinute());
                config.setWindowSeconds(60);
            }
            default -> {
                config.setMaxRequests(100);
                config.setWindowSeconds(1);
            }
        }
        return config;
    }

    private String buildRateLimitKey(RateLimitScope scope, Long tenantId, Long userId, String clientIp, String path) {
        String identifier;
        switch (scope) {
            case TENANT -> identifier = String.valueOf(tenantId);
            case USER -> identifier = String.valueOf(userId);
            case LOGIN_IP -> identifier = clientIp == null ? "unknown" : clientIp.replace(':', '_');
            default -> identifier = "global";
        }
        String resource = "*";
        if (path != null && !path.isBlank()) {
            resource = path.replace('/', ':').replaceAll("^:+|:+$", "");
        }
        return "jinshu:ratelimit:" + scope.name().toLowerCase() + ":" + identifier + ":" + resource;
    }

    private String buildConcurrencyKey(Long tenantId) {
        return "jinshu:ratelimit:concurrent:tenant:" + tenantId;
    }
}
