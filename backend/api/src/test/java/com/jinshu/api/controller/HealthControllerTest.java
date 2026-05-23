package com.jinshu.api.controller;

import com.jinshu.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthController 健康检查控制器测试")
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @InjectMocks
    private HealthController healthController;

    @Test
    @DisplayName("GET /health 返回 Result，status=UP")
    void given_noDb_when_health_then_returnUp() {
        Result<?> result = healthController.health();

        assertThat(result.getCode()).isZero();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.getData();
        assertThat(data.get("status")).isEqualTo("UP");
        assertThat(data.get("service")).isEqualTo("jinshu-report-api");
        assertThat(data.get("database")).isInstanceOf(Boolean.class);
    }

    @Test
    @DisplayName("GET /ready 就绪探针返回 NOT_READY（无数据库连接）")
    void given_noDb_when_ready_then_returnNotReady() {
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response =
                healthController.ready();

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().get("status")).isEqualTo("NOT_READY");
        assertThat(response.getBody().get("database")).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("GET /live 存活探针返回 ALIVE")
    void given_noDb_when_live_then_returnAlive() {
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response =
                healthController.live();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("status")).isEqualTo("ALIVE");
    }
}
