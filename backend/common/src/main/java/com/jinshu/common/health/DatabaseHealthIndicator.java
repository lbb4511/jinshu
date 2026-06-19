package com.jinshu.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库健康检查指示器。
 *
 * 当应用上下文中存在 {@link DataSource} Bean 时启用，
 * 尝试获取连接并校验其有效性。
 */
@Slf4j
@Component
@ConditionalOnBean(DataSource.class)
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return Health.up()
                        .withDetail("database", "reachable")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "connection invalid")
                    .build();
        } catch (Exception e) {
            log.warn("Database health check failed", e);
            return Health.down(e)
                    .withDetail("database", "unreachable")
                    .build();
        }
    }
}
