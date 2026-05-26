package com.jinshu.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置类
 *
 * Spring Boot 4 默认使用 Jackson 3（tools.jackson 包），
 * 但项目代码使用 Jackson 2（com.fasterxml.jackson 包）。
 * 此处显式声明 Jackson 2 的 ObjectMapper Bean，供业务代码注入使用。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
