package com.jinshu.api.integration;

import com.jinshu.api.dao.DataSourceMapper;
import com.jinshu.api.service.DataSourceService;
import com.jinshu.common.entity.DataSource;
import com.jinshu.common.security.KeyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 DataSource.connection_config 字段通过 EncryptTypeHandler 透明加解密。
 */
@DisplayName("DataSource encryption - DB integration tests")
class DataSourceEncryptionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private KeyManager keyManager;

    @Autowired
    private javax.sql.DataSource dbDataSource;

    @Test
    @DisplayName("Should encrypt connection_config in database and decrypt via mapper")
    @Transactional
    void shouldEncryptInDbAndDecryptViaMapper() {
        // 1. 创建数据源
        DataSourceService.CreateDataSourceRequest request = new DataSourceService.CreateDataSourceRequest();
        request.setName("加密测试数据源");
        request.setType("POSTGRESQL");
        request.setHost("localhost");
        request.setPort(5432);
        request.setDatabaseName("testdb");
        request.setUsername("dbuser");
        request.setPassword("superSecretPassword123");
        request.setSslEnabled(true);
        request.setDescription("用于验证字段级加密");

        DataSource created = dataSourceService.createDataSource(request);
        assertThat(created.getId()).isNotNull();

        // 2. 直接从数据库读取 connection_config，应为密文
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dbDataSource);
        String rawDbValue = jdbcTemplate.queryForObject(
                "SELECT connection_config::text FROM meta.data_source WHERE id = ?",
                String.class,
                created.getId()
        );

        assertThat(rawDbValue).isNotNull();
        assertThat(rawDbValue).isNotEqualTo(created.getConnectionConfig());
        assertThat(keyManager.isEncrypted(rawDbValue)).isTrue();

        // 3. 通过 Mapper 查询，应自动解密为明文 JSON
        DataSource fromMapper = dataSourceMapper.selectByIdAndTenantId(created.getId(), TENANT_ID);
        assertThat(fromMapper).isNotNull();
        assertThat(fromMapper.getConnectionConfig()).isEqualTo(created.getConnectionConfig());
        assertThat(fromMapper.getConnectionConfig()).contains("superSecretPassword123");
    }
}
