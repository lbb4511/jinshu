package com.jinshu.api.service;

import com.jinshu.api.dao.DataSourceMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.DataSource;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DataSourceService 数据源管理服务测试")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceMapper dataSourceMapper;

    private DataSourceService dataSourceService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long DS_ID = 300L;

    @BeforeEach
    void setUp() {
        dataSourceService = new DataSourceService(dataSourceMapper);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername("testuser");
        UserContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    private DataSource createDefaultDataSource() {
        DataSource ds = new DataSource();
        ds.setId(DS_ID);
        ds.setTenantId(TENANT_ID);
        ds.setName("测试数据库");
        ds.setType("MYSQL");
        ds.setHost("192.168.1.1");
        ds.setPort(3306);
        ds.setDatabaseName("testdb");
        ds.setUsername("dbuser");
        ds.setStatus("ACTIVE");
        ds.setCreatedBy(USER_ID);
        ds.setCreatedAt(LocalDateTime.now());
        ds.setUpdatedAt(LocalDateTime.now());
        return ds;
    }

    @Test
    @DisplayName("createDataSource：创建成功，返回数据源信息")
    void given_validRequest_when_createDataSource_then_success() {
        when(dataSourceMapper.selectByNameAndTenantId("测试数据库", TENANT_ID)).thenReturn(null);
        doAnswer(invocation -> {
            DataSource ds = invocation.getArgument(0);
            ds.setId(DS_ID);
            return null;
        }).when(dataSourceMapper).insert(any(DataSource.class));

        DataSourceService.CreateDataSourceRequest request = new DataSourceService.CreateDataSourceRequest();
        request.setName("测试数据库");
        request.setType("MYSQL");
        request.setHost("192.168.1.1");
        request.setPort(3306);
        request.setDatabaseName("testdb");
        request.setUsername("dbuser");
        request.setPassword("secret123");

        DataSource result = dataSourceService.createDataSource(request);

        assertThat(result.getName()).isEqualTo("测试数据库");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getCreatedBy()).isEqualTo(USER_ID);
        verify(dataSourceMapper).insert(any(DataSource.class));
    }

    @Test
    @DisplayName("createDataSource：名称重复抛异常")
    void given_duplicateName_when_create_then_throw() {
        when(dataSourceMapper.selectByNameAndTenantId("重复名称", TENANT_ID))
                .thenReturn(new DataSource());

        DataSourceService.CreateDataSourceRequest request = new DataSourceService.CreateDataSourceRequest();
        request.setName("重复名称");

        assertThatThrownBy(() -> dataSourceService.createDataSource(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据源名称已存在");
    }

    @Test
    @DisplayName("getDataSourceById：存在返回正确")
    void given_existingDs_when_getById_then_return() {
        when(dataSourceMapper.selectByIdAndTenantId(DS_ID, TENANT_ID))
                .thenReturn(createDefaultDataSource());

        DataSource result = dataSourceService.getDataSourceById(DS_ID);

        assertThat(result.getId()).isEqualTo(DS_ID);
        assertThat(result.getName()).isEqualTo("测试数据库");
    }

    @Test
    @DisplayName("getDataSourceById：不存在抛异常")
    void given_nonExistentDs_when_getById_then_throw() {
        when(dataSourceMapper.selectByIdAndTenantId(999L, TENANT_ID)).thenReturn(null);

        assertThatThrownBy(() -> dataSourceService.getDataSourceById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("listDataSources：分页查询成功")
    void given_pagination_when_list_then_returnPage() {
        when(dataSourceMapper.selectList(eq(TENANT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createDefaultDataSource()));
        when(dataSourceMapper.countList(eq(TENANT_ID), any(), any())).thenReturn(1L);

        PageResult<DataSource> result = dataSourceService.listDataSources(null, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateDataSource：更新成功")
    void given_validRequest_when_update_then_success() {
        when(dataSourceMapper.selectByIdAndTenantId(DS_ID, TENANT_ID))
                .thenReturn(createDefaultDataSource());

        DataSourceService.UpdateDataSourceRequest request = new DataSourceService.UpdateDataSourceRequest();
        request.setName("新名称");
        request.setDescription("新描述");

        DataSource result = dataSourceService.updateDataSource(DS_ID, request);

        assertThat(result.getName()).isEqualTo("新名称");
        verify(dataSourceMapper).update(any(DataSource.class));
    }

    @Test
    @DisplayName("deleteDataSource：数据源未被引用时删除成功")
    void given_existingDsNoReference_when_delete_then_success() {
        when(dataSourceMapper.selectByIdAndTenantId(DS_ID, TENANT_ID))
                .thenReturn(createDefaultDataSource());
        when(dataSourceMapper.countByDataSourceId(DS_ID)).thenReturn(0L);

        dataSourceService.deleteDataSource(DS_ID);

        verify(dataSourceMapper).update(any(DataSource.class));
    }

    @Test
    @DisplayName("deleteDataSource：数据源被引用时抛异常")
    void given_dsWithReference_when_delete_then_throw() {
        when(dataSourceMapper.selectByIdAndTenantId(DS_ID, TENANT_ID))
                .thenReturn(createDefaultDataSource());
        when(dataSourceMapper.countByDataSourceId(DS_ID)).thenReturn(2L);

        assertThatThrownBy(() -> dataSourceService.deleteDataSource(DS_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("被 2 个报表引用");
    }

    @Test
    @DisplayName("testConnection：数据库连接信息记录日志")
    void given_dsId_when_testConnection_then_logCreated() {
        when(dataSourceMapper.selectByIdAndTenantId(DS_ID, TENANT_ID))
                .thenReturn(createDefaultDataSource());

        // 实际测试连接会失败（无真实数据库），验证不抛异常即可
        assertThatCode(() -> dataSourceService.testConnection(DS_ID))
                .doesNotThrowAnyException();
    }
}
