package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TenantMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @AuditLog(operation = "CREATE_TENANT", targetType = "TENANT")
    public Tenant createTenant(CreateTenantRequest request) {
        Tenant existing = tenantMapper.selectByCode(request.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "租户编码已存在");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setCode(request.getCode());
        tenant.setStatus("ACTIVE");
        tenant.setDescription(request.getDescription());
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        if (request.getQuotaConfig() != null) {
            try {
                tenant.setQuotaConfig(objectMapper.writeValueAsString(request.getQuotaConfig()));
            } catch (Exception e) {
                log.error("Failed to serialize quota config", e);
            }
        }

        tenantMapper.insert(tenant);

        if (request.getAdminUsername() != null && request.getAdminPassword() != null) {
            User admin = new User();
            admin.setTenantId(tenant.getId());
            admin.setUsername(request.getAdminUsername());
            admin.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
            admin.setEmail(request.getAdminEmail());
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(admin);
        }

        return tenant;
    }

    public Tenant getTenantById(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    public Tenant getTenantByCode(String code) {
        Tenant tenant = tenantMapper.selectByCode(code);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    public PageResult<Tenant> listTenants(String name, String status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Tenant> tenants = tenantMapper.selectList(name, status, offset, pageSize);
        long total = tenantMapper.countList(name, status);
        return PageResult.of(tenants, total, page, pageSize);
    }

    @AuditLog(operation = "UPDATE_TENANT", targetType = "TENANT")
    public Tenant updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = getTenantById(id);

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tenant.setDescription(request.getDescription());
        }
        if (request.getQuotaConfig() != null) {
            try {
                tenant.setQuotaConfig(objectMapper.writeValueAsString(request.getQuotaConfig()));
            } catch (Exception e) {
                log.error("Failed to serialize quota config", e);
            }
        }
        tenant.setUpdatedAt(LocalDateTime.now());

        tenantMapper.update(tenant);
        return tenant;
    }

    @AuditLog(operation = "CHANGE_TENANT_STATUS", targetType = "TENANT")
    public Tenant changeTenantStatus(Long id, String status) {
        Tenant tenant = getTenantById(id);

        if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的租户状态");
        }

        tenant.setStatus(status);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);
        return tenant;
    }

    @AuditLog(operation = "ARCHIVE_TENANT", targetType = "TENANT")
    public void archiveTenant(Long id) {
        Tenant tenant = getTenantById(id);
        tenant.setStatus("ARCHIVED");
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);
    }

    public Map<String, Object> getQuotaUsage(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        Map<String, Object> result = new HashMap<>();

        result.put("tenantId", tenantId);
        result.put("quotaConfig", tenant.getQuotaConfig());

        long userCount = userMapper.countByTenantId(tenantId);
        result.put("userCount", userCount);

        return result;
    }

    public static class CreateTenantRequest {
        private String name;
        private String code;
        private String adminUsername;
        private String adminPassword;
        private String adminEmail;
        private Map<String, Object> quotaConfig;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getAdminUsername() { return adminUsername; }
        public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getAdminEmail() { return adminEmail; }
        public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
        public Map<String, Object> getQuotaConfig() { return quotaConfig; }
        public void setQuotaConfig(Map<String, Object> quotaConfig) { this.quotaConfig = quotaConfig; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdateTenantRequest {
        private String name;
        private String description;
        private Map<String, Object> quotaConfig;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getQuotaConfig() { return quotaConfig; }
        public void setQuotaConfig(Map<String, Object> quotaConfig) { this.quotaConfig = quotaConfig; }
    }
}
