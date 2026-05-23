package com.jinshu.api.service;

import com.jinshu.api.dao.TenantMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TenantService 租户管理服务测试")
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TenantService tenantService;

    private static final Long TENANT_ID = 100L;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantMapper, userMapper, passwordEncoder);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(1L);
        UserContext.setUsername("admin");
        UserContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("createTenant：创建租户成功，返回租户信息")
    void given_validRequest_when_createTenant_then_success() {
        when(tenantMapper.selectByCode("test-code")).thenReturn(null);
        doAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            t.setId(TENANT_ID);
            return null;
        }).when(tenantMapper).insert(any(Tenant.class));

        TenantService.CreateTenantRequest request = new TenantService.CreateTenantRequest();
        request.setName("测试租户");
        request.setCode("test-code");
        request.setDescription("测试");

        Tenant result = tenantService.createTenant(request);

        assertThat(result.getName()).isEqualTo("测试租户");
        assertThat(result.getCode()).isEqualTo("test-code");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(tenantMapper).insert(any(Tenant.class));
    }

    @Test
    @DisplayName("createTenant：编码重复抛异常")
    void given_duplicateCode_when_createTenant_then_throw() {
        when(tenantMapper.selectByCode("dup-code")).thenReturn(new Tenant());

        TenantService.CreateTenantRequest request = new TenantService.CreateTenantRequest();
        request.setCode("dup-code");

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户编码已存在");
    }

    @Test
    @DisplayName("getTenantById：存在的租户返回正确")
    void given_existingTenant_when_getById_then_return() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("测试租户");
        when(tenantMapper.selectById(TENANT_ID)).thenReturn(tenant);

        Tenant result = tenantService.getTenantById(TENANT_ID);

        assertThat(result.getId()).isEqualTo(TENANT_ID);
        assertThat(result.getName()).isEqualTo("测试租户");
    }

    @Test
    @DisplayName("getTenantById：不存在的租户抛异常")
    void given_nonExistentTenant_when_getById_then_throw() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> tenantService.getTenantById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("listTenants：分页查询租户列表")
    void given_pagination_when_listTenants_then_returnPage() {
        Tenant t1 = new Tenant();
        t1.setId(1L);
        t1.setName("租户1");
        Tenant t2 = new Tenant();
        t2.setId(2L);
        t2.setName("租户2");

        when(tenantMapper.selectList(isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(t1, t2));
        when(tenantMapper.countList(isNull(), isNull())).thenReturn(2L);

        PageResult<Tenant> result = tenantService.listTenants(null, null, 1, 20);

        assertThat(result.getList()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateTenantStatus：挂起租户状态变更")
    void given_validTransition_when_updateStatus_then_success() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setStatus("ACTIVE");
        when(tenantMapper.selectById(TENANT_ID)).thenReturn(tenant);

        tenantService.changeTenantStatus(TENANT_ID, "SUSPENDED");

        verify(tenantMapper).update(argThat(t ->
                "SUSPENDED".equals(t.getStatus())));
    }
}
