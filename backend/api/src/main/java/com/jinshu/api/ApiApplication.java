package com.jinshu.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 锦书企业级报表系统 - API服务启动类
 *
 * 核心功能：
 * - 提供RESTful API接口
 * - 多租户数据隔离
 * - 报表全生命周期管理
 * - 高性能PDF导出
 */
@SpringBootApplication(scanBasePackages = "com.jinshu")
public class ApiApplication {

    /**
     * 应用入口函数
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
