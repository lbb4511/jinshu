package com.jinshu.batch.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"com.jinshu.batch.dao", "com.jinshu.common.dao"})
public class MyBatisConfig {
}
