package com.jinshu.api.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.jinshu.api.dao")
public class MyBatisConfig {

}
