package com.jinshu.datasync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 锦书企业级报表系统 DataSync 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.jinshu")
public class DataSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSyncApplication.class, args);
    }
}
