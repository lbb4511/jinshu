package com.jinshu.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 健康检查指示器。
 *
 * 当应用上下文中存在 {@link RedisConnectionFactory} Bean 时启用，
 * 尝试获取 Redis 连接。
 */
@Slf4j
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (var connection = connectionFactory.getConnection()) {
            connection.ping();
            return Health.up()
                    .withDetail("redis", "reachable")
                    .build();
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            return Health.down(e)
                    .withDetail("redis", "unreachable")
                    .build();
        }
    }
}
