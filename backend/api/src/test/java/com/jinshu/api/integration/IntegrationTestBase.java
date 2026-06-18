package com.jinshu.api.integration;

import com.jinshu.api.ApiApplication;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig
public abstract class IntegrationTestBase {

    protected static final Long TENANT_ID = 1L;
    protected static final Long USER_ID = 1L;
    protected static final String USERNAME = "testuser";
    protected static final String ROLE = "ADMIN";

    @Autowired
    private Flyway flyway;

    @BeforeAll
    void cleanAndMigrateDatabase() {
        flyway.clean();
        flyway.migrate();
    }

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
    }
}
