package com.jinshu.common;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * common 模块测试用 Spring Boot 应用入口。
 *
 * 排除 Redis 自动配置，并提供一个 Mock StringRedisTemplate，
 * 使 Actuator 端点测试可在无外部依赖环境下运行。
 */
@SpringBootApplication(exclude = {
        DataRedisAutoConfiguration.class
})
public class CommonTestApplication {

    @Configuration
    static class TestRedisConfig {

        @Bean
        public StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }
    }
}
