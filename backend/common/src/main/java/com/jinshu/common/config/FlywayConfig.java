package com.jinshu.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * 显式配置 Flyway（Spring Boot 4 未内置 Flyway 自动配置）。
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.default-schema:}")
    private String defaultSchema;

    @Value("${spring.flyway.schemas:}")
    private List<String> schemas;

    @Value("${spring.flyway.baseline-on-migrate:false}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:1}")
    private String baselineVersion;

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    @Value("${spring.flyway.create-schemas:false}")
    private boolean createSchemas;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        org.flywaydb.core.api.configuration.FluentConfiguration configure = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .cleanDisabled(cleanDisabled)
                .createSchemas(createSchemas);

        if (defaultSchema != null && !defaultSchema.isEmpty()) {
            configure.defaultSchema(defaultSchema);
        }
        if (schemas != null && !schemas.isEmpty()) {
            configure.schemas(schemas.toArray(new String[0]));
        }
        configure.baselineVersion(baselineVersion);

        return configure.load();
    }
}
