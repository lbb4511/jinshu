package com.jinshu.api.config;

import com.jinshu.api.config.TenantInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.jinshu.api.dao")
public class MyBatisConfig {

    @Bean
    @ConditionalOnProperty(name = "jinshu.tenant.interceptor.enabled", havingValue = "true", matchIfMissing = true)
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor();
    }
}
