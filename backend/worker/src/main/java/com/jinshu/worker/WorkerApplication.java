package com.jinshu.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 锦书企业级报表系统 Worker 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.jinshu")
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
