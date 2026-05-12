package com.jinshu.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 锦书企业级报表系统 API 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.jinshu")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
