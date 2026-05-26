package com.jinshu.api.integration;

import com.jinshu.api.dao.UserMapper;
import com.jinshu.api.service.TenantService;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantService - DB integration tests")
class TenantServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserMapper userMapper;

    @Nested
    @DisplayName("Create tenant")
    @Transactional
    class Create {

        @Test
        @DisplayName("Should create tenant with admin user")
        void shouldCreateTenantWithAdmin() {
            var request = new TenantService.CreateTenantRequest();
            request.setName("测试租户");
            request.setCode("test-tenant");
            request.setAdminUsername("admin");
            request.setAdminPassword("Admin@123");
            request.setAdminEmail("admin@test.com");

            Tenant tenant = tenantService.createTenant(request);

            assertThat(tenant.getId()).isNotNull();
            assertThat(tenant.getCode()).isEqualTo("test-tenant");
            assertThat(tenant.getStatus()).isEqualTo("ACTIVE");

            User admin = userMapper.selectByUsernameAndTenantId("admin", tenant.getId());
            assertThat(admin).isNotNull();
            assertThat(admin.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should create tenant without admin user")
        void shouldCreateTenantWithoutAdmin() {
            var request = new TenantService.CreateTenantRequest();
            request.setName("纯租户");
            request.setCode("pure-tenant");

            Tenant tenant = tenantService.createTenant(request);
            assertThat(tenant.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should reject duplicate code")
        void shouldRejectDuplicateCode() {
            var request = new TenantService.CreateTenantRequest();
            request.setName("租户A");
            request.setCode("dup-code");
            tenantService.createTenant(request);

            var duplicate = new TenantService.CreateTenantRequest();
            duplicate.setName("租户B");
            duplicate.setCode("dup-code");
            assertThatThrownBy(() -> tenantService.createTenant(duplicate))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Get tenant")
    @Transactional
    class Get {

        private Tenant saved;

        @BeforeEach
        void setUp() {
            var request = new TenantService.CreateTenantRequest();
            request.setName("查询测试");
            request.setCode("get-test");
            saved = tenantService.createTenant(request);
        }

        @Test
        @DisplayName("Should find by id")
        void shouldGetById() {
            Tenant found = tenantService.getTenantById(saved.getId());
            assertThat(found.getCode()).isEqualTo("get-test");
        }

        @Test
        @DisplayName("Should find by code")
        void shouldGetByCode() {
            Tenant found = tenantService.getTenantByCode("get-test");
            assertThat(found.getName()).isEqualTo("查询测试");
        }

        @Test
        @DisplayName("Should throw when not found")
        void shouldThrowWhenNotFound() {
            assertThatThrownBy(() -> tenantService.getTenantById(99999L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("List tenants")
    @Transactional
    class ListTenants {

        @BeforeEach
        void setUp() {
            for (int i = 0; i < 3; i++) {
                var req = new TenantService.CreateTenantRequest();
                req.setName("租户" + i);
                req.setCode("list-" + i);
                tenantService.createTenant(req);
            }
        }

        @Test
        @DisplayName("Should list all tenants")
        void shouldListAll() {
            PageResult<Tenant> result = tenantService.listTenants(null, null, 1, 10);
            assertThat(result.getList()).hasSize(3);
        }

        @Test
        @DisplayName("Should filter by name")
        void shouldFilterByName() {
            PageResult<Tenant> result = tenantService.listTenants("租户1", null, 1, 10);
            assertThat(result.getList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update tenant")
    @Transactional
    class Update {

        private Tenant saved;

        @BeforeEach
        void setUp() {
            var req = new TenantService.CreateTenantRequest();
            req.setName("旧名称");
            req.setCode("update-test");
            saved = tenantService.createTenant(req);
        }

        @Test
        @DisplayName("Should update name and description")
        void shouldUpdate() {
            var updateReq = new TenantService.UpdateTenantRequest();
            updateReq.setName("新名称");

            Tenant updated = tenantService.updateTenant(saved.getId(), updateReq);
            assertThat(updated.getName()).isEqualTo("新名称");
        }
    }

    @Nested
    @DisplayName("Status management")
    @Transactional
    class Status {

        private Tenant saved;

        @BeforeEach
        void setUp() {
            var req = new TenantService.CreateTenantRequest();
            req.setName("状态测试");
            req.setCode("status-test");
            saved = tenantService.createTenant(req);
        }

        @Test
        @DisplayName("Should change status")
        void shouldChangeStatus() {
            Tenant suspended = tenantService.changeTenantStatus(saved.getId(), "SUSPENDED");
            assertThat(suspended.getStatus()).isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("Should archive tenant")
        void shouldArchive() {
            tenantService.archiveTenant(saved.getId());
            Tenant archived = tenantService.getTenantById(saved.getId());
            assertThat(archived.getStatus()).isEqualTo("ARCHIVED");
        }

        @Test
        @DisplayName("Should reject invalid status")
        void shouldRejectInvalidStatus() {
            assertThatThrownBy(() -> tenantService.changeTenantStatus(saved.getId(), "INVALID"))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Quota usage")
    @Transactional
    class Quota {

        @Test
        @DisplayName("Should return quota usage map")
        void shouldGetQuotaUsage() {
            var req = new TenantService.CreateTenantRequest();
            req.setName("配额测试");
            req.setCode("quota-test");
            Tenant tenant = tenantService.createTenant(req);

            Map<String, Object> usage = tenantService.getQuotaUsage(tenant.getId());
            assertThat(usage).containsKeys("tenantId", "quotaConfig", "userCount");
            assertThat(usage.get("userCount")).isEqualTo(0L);
        }
    }
}
