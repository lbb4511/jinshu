package com.jinshu.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 锦书企业级报表系统 Batch 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.jinshu")
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
