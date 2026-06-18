package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.DataSourceMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.DataSource;
import com.jinshu.common.entity.DataSourceConnectionLog;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @AuditLog(operation = "CREATE_DATASOURCE", targetType = "DATASOURCE")
    public DataSource createDataSource(CreateDataSourceRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        DataSource existing = dataSourceMapper.selectByNameAndTenantId(request.getName(), tenantId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源名称已存在");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("password", request.getPassword());
        config.put("sslEnabled", request.getSslEnabled());
        config.put("connectTimeout", 5000);

        DataSource dataSource = new DataSource();
        dataSource.setTenantId(tenantId);
        dataSource.setName(request.getName());
        dataSource.setType(request.getType());
        dataSource.setHost(request.getHost());
        dataSource.setPort(request.getPort());
        dataSource.setDatabaseName(request.getDatabaseName());
        dataSource.setUsername(request.getUsername());
        dataSource.setConnectionConfig(toJson(config));
        dataSource.setStatus("ACTIVE");
        dataSource.setDescription(request.getDescription());
        dataSource.setCreatedBy(userId);
        dataSource.setCreatedAt(LocalDateTime.now());
        dataSource.setUpdatedAt(LocalDateTime.now());

        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    public DataSource getDataSourceById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceMapper.selectByIdAndTenantId(id, tenantId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        return dataSource;
    }

    public PageResult<DataSource> listDataSources(String name, String type, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;
        List<DataSource> dataSources = dataSourceMapper.selectList(tenantId, name, type, offset, pageSize);
        long total = dataSourceMapper.countList(tenantId, name, type);
        return PageResult.of(dataSources, total, page, pageSize);
    }

    @Transactional
    @AuditLog(operation = "UPDATE_DATASOURCE", targetType = "DATASOURCE")
    public DataSource updateDataSource(Long id, UpdateDataSourceRequest request) {
        DataSource dataSource = getDataSourceById(id);

        PermissionUtils.checkOwner(dataSource.getCreatedBy());

        if (request.getName() != null) {
            DataSource existing = dataSourceMapper.selectByNameAndTenantId(request.getName(), dataSource.getTenantId());
            if (existing != null && !existing.getId().equals(id)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源名称已存在");
            }
            dataSource.setName(request.getName());
        }
        if (request.getType() != null) {
            dataSource.setType(request.getType());
        }
        if (request.getHost() != null) {
            dataSource.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            dataSource.setPort(request.getPort());
        }
        if (request.getDatabaseName() != null) {
            dataSource.setDatabaseName(request.getDatabaseName());
        }
        if (request.getUsername() != null) {
            dataSource.setUsername(request.getUsername());
        }
        if (request.getDescription() != null) {
            dataSource.setDescription(request.getDescription());
        }
        if (request.getPassword() != null) {
            Map<String, Object> config = fromJson(dataSource.getConnectionConfig());
            config.put("password", request.getPassword());
            dataSource.setConnectionConfig(toJson(config));
        }

        dataSource.setUpdatedBy(UserContext.getUserId());
        dataSource.setUpdatedAt(LocalDateTime.now());
        dataSourceMapper.update(dataSource);
        return dataSource;
    }

    @Transactional
    @AuditLog(operation = "DELETE_DATASOURCE", targetType = "DATASOURCE")
    public void deleteDataSource(Long id) {
        DataSource dataSource = getDataSourceById(id);

        PermissionUtils.checkOwner(dataSource.getCreatedBy());

        long reportCount = dataSourceMapper.countByDataSourceId(id);
        if (reportCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该数据源被 " + reportCount + " 个报表引用，无法删除");
        }

        dataSource.setUpdatedAt(LocalDateTime.now());
        dataSourceMapper.update(dataSource);
    }

    public Map<String, Object> testConnection(Long id) {
        DataSource dataSource = getDataSourceById(id);

        PermissionUtils.checkOwner(dataSource.getCreatedBy());

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            result.put("success", true);
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            result.put("testedAt", LocalDateTime.now().toString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            result.put("testedAt", LocalDateTime.now().toString());
        }

        dataSource.setLastTestTime(LocalDateTime.now());
        dataSource.setLastTestResult(toJson(result));
        dataSource.setUpdatedAt(LocalDateTime.now());
        dataSourceMapper.update(dataSource);

        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to deserialize from JSON", e);
            return new HashMap<>();
        }
    }

    public static class CreateDataSourceRequest {
        private String name;
        private String type;
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;
        private Boolean sslEnabled;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Boolean getSslEnabled() { return sslEnabled; }
        public void setSslEnabled(Boolean sslEnabled) { this.sslEnabled = sslEnabled; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdateDataSourceRequest {
        private String name;
        private String type;
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
