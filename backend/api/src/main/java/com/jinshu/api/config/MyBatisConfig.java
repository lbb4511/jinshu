package com.jinshu.api.config;

import com.jinshu.common.security.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.jinshu.dao")
@RequiredArgsConstructor
public class MyBatisConfig {

    private final TenantInterceptor tenantInterceptor;

    @Bean
    public org.apache.ibatis.plugin.Interceptor tenantInterceptorPlugin() {
        return tenantInterceptor;
    }
}
