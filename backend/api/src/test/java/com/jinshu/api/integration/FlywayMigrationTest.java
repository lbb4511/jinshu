package com.jinshu.api.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Flyway 迁移脚本在本地 PostgreSQL 测试实例上可正常执行。
 * 运行前请确保本地有 PostgreSQL 实例监听 127.0.0.1:5433，数据库 jinshu 已存在：
 * <pre>
 *   docker run --name jinshu-pg-test -e POSTGRES_USER=jinshu -e POSTGRES_PASSWORD=jinshu123 \
 *     -e POSTGRES_DB=jinshu -p 5433:5432 -d postgres:16-alpine
 * </pre>
 */
@DisplayName("Flyway migration scripts")
class FlywayMigrationTest {

    @Test
    @DisplayName("should apply all migrations on a fresh database")
    void shouldApplyAllMigrations() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:5433/jinshu");
        config.setUsername("jinshu");
        config.setPassword("jinshu123");
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(2);
        config.setConnectionInitSql("SET search_path TO sys,meta,data,workflow,audit,task,public");
        DataSource dataSource = new HikariDataSource(config);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .defaultSchema("public")
                .schemas("sys", "meta", "data", "workflow", "audit", "task")
                .createSchemas(true)
                .load();

        var result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(5);
        assertThat(result.success).isTrue();

        // 验证关键表已创建
        assertThat(tableExists(dataSource, "sys", "tenant")).isTrue();
        assertThat(tableExists(dataSource, "sys", "users")).isTrue();
        assertThat(tableExists(dataSource, "meta", "report_metadata")).isTrue();
        assertThat(tableExists(dataSource, "data", "import_error_log")).isTrue();
        assertThat(tableExists(dataSource, "task", "task")).isTrue();
        assertThat(tableExists(dataSource, "audit", "audit_log")).isTrue();
        assertThat(tableExists(dataSource, "public", "flyway_schema_history")).isTrue();
    }

    private boolean tableExists(DataSource dataSource, String schema, String table) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(
                     "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check table existence: " + schema + "." + table, e);
        }
    }
}
