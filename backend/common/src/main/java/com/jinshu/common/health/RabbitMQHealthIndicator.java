package com.jinshu.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 健康检查指示器。
 *
 * 当应用上下文中存在 {@link ConnectionFactory} Bean 时启用，
 * 尝试创建 RabbitMQ 连接。
 */
@Slf4j
@Component
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitMQHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    public RabbitMQHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (var connection = connectionFactory.createConnection()) {
            if (connection.isOpen()) {
                return Health.up()
                        .withDetail("rabbitmq", "reachable")
                        .build();
            }
            return Health.down()
                    .withDetail("rabbitmq", "connection closed")
                    .build();
        } catch (Exception e) {
            log.warn("RabbitMQ health check failed", e);
            return Health.down(e)
                    .withDetail("rabbitmq", "unreachable")
                    .build();
        }
    }
}
