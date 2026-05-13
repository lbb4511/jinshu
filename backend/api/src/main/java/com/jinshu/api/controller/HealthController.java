package com.jinshu.api.controller;

import com.jinshu.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查控制器
 *
 * 提供Kubernetes探针接口：
 * - /health: 综合健康检查（服务 + 数据库）
 * - /ready: 就绪探针（Pod是否可接收流量）
 * - /live: 存活探针（Pod是否需要重启）
 *
 * 前端也会调用/health显示系统状态
 */
@RestController
@RequestMapping
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * 综合健康检查接口
     *
     * 返回内容：
     * - status: 服务状态 (UP)
     * - service: 服务名称
     * - database: 数据库连接状态 (true/false)
     *
     * @return 健康检查结果
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "jinshu-report-api");
        status.put("database", checkDatabase());
        return Result.success(status);
    }

    /**
     * 就绪探针 - Kubernetes Readiness Probe
     *
     * 用于判断Pod是否可以接收流量
     * 只有数据库连接正常时返回200
     *
     * @return 就绪状态
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> status = new HashMap<>();
        boolean dbOk = checkDatabase();

        status.put("status", dbOk ? "READY" : "NOT_READY");
        status.put("database", dbOk ? "UP" : "DOWN");

        return dbOk ? ResponseEntity.ok(status) : ResponseEntity.status(503).body(status);
    }

    /**
     * 存活探针 - Kubernetes Liveness Probe
     *
     * 用于判断Pod是否需要重启
     * 只要服务能响应即返回200
     *
     * @return 存活状态
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ALIVE");
        status.put("service", "jinshu-report-api");
        return ResponseEntity.ok(status);
    }

    /**
     * 检查数据库连接状态
     *
     * @return 连接是否正常
     */
    private boolean checkDatabase() {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
