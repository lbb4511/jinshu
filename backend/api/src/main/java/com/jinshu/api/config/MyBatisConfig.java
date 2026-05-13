package com.jinshu.api.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 *
 * 功能说明：
 * - 配置Mapper接口扫描路径
 * - 自动注册Mapper接口为Spring Bean
 *
 * 注意事项：
 * - 后续可扩展添加多租户拦截器
 * - 可添加分页插件配置
 * - 可添加SQL日志配置
 */
@Configuration
@MapperScan("com.jinshu.api.dao")
public class MyBatisConfig {

}
