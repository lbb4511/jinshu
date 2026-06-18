package com.jinshu.api.config;

import com.jinshu.common.security.EncryptTypeHandler;
import com.jinshu.common.security.KeyManager;
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

    /**
     * 敏感字段加密 TypeHandler。
     * 声明为 Spring Bean 后，MyBatis Spring Boot 自动配置会将其注册到 SqlSessionFactory。
     */
    @Bean
    public EncryptTypeHandler encryptTypeHandler(KeyManager keyManager) {
        return new EncryptTypeHandler(keyManager);
    }
}
