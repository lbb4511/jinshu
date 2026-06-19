package com.jinshu.common.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("自定义健康检查指示器测试")
class HealthIndicatorTest {

    @Test
    @DisplayName("DatabaseHealthIndicator：连接有效时返回 UP")
    void given_validConnection_when_check_then_up() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("DatabaseHealthIndicator：连接无效时返回 DOWN")
    void given_invalidConnection_when_check_then_down() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("DatabaseHealthIndicator：获取连接异常时返回 DOWN")
    void given_connectionException_when_check_then_down() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
