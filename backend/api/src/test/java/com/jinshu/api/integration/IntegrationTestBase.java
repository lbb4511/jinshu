package com.jinshu.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.ApiApplication;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


import static org.mockito.Mockito.mock;

@SpringBootTest(
    classes = {ApiApplication.class, IntegrationTestBase.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@SpringJUnitConfig
public abstract class IntegrationTestBase {

    protected static final Long TENANT_ID = 1L;
    protected static final Long USER_ID = 1L;
    protected static final String USERNAME = "testuser";
    protected static final String ROLE = "ADMIN";

    @BeforeEach
    void setUpBaseContext() {
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername(USERNAME);
        UserContext.setRole(ROLE);
    }

    @AfterEach
    void tearDownBaseContext() {
        TenantContext.clear();
        UserContext.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        public StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
