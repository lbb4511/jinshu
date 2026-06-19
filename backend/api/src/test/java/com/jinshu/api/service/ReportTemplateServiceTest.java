package com.jinshu.api.service;

import com.jinshu.api.dao.ReportTemplateMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.ReportTemplate;
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

@DisplayName("ReportTemplateService 报表模板服务测试")
@ExtendWith(MockitoExtension.class)
class ReportTemplateServiceTest {

    @Mock
    private ReportTemplateMapper reportTemplateMapper;

    @Mock
    private ReportService reportService;

    private ReportTemplateService reportTemplateService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long TEMPLATE_ID = 300L;
    private static final Long REPORT_ID = 500L;

    @BeforeEach
    void setUp() {
        reportTemplateService = new ReportTemplateService(reportTemplateMapper, reportService);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername("testuser");
        UserContext.setRole("USER");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    private ReportTemplate createTemplate() {
        ReportTemplate template = new ReportTemplate();
        template.setId(TEMPLATE_ID);
        template.setTenantId(TENANT_ID);
        template.setName("测试模板");
        template.setDescription("模板描述");
        template.setCategory("SALES");
        template.setLayoutJson("{\"version\":1}");
        template.setSampleData("{\"rows\":[]}");
        template.setIsPublic(false);
        template.setIsSystem(false);
        template.setStatus("ACTIVE");
        template.setCreatedBy(USER_ID);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return template;
    }

    // ============ createTemplate ============

    @Test
    @DisplayName("createTemplate：创建模板成功")
    void given_validRequest_when_createTemplate_then_success() {
        when(reportTemplateMapper.insert(any(ReportTemplate.class))).thenAnswer(invocation -> {
            ReportTemplate t = invocation.getArgument(0);
            t.setId(TEMPLATE_ID);
            return 1;
        });

        ReportTemplateService.CreateTemplateRequest request = new ReportTemplateService.CreateTemplateRequest();
        request.setName("销售模板");
        request.setDescription("季度销售模板");
        request.setCategory("SALES");
        request.setLayoutJson("{\"version\":1}");

        ReportTemplate result = reportTemplateService.createTemplate(request);

        assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
        assertThat(result.getName()).isEqualTo("销售模板");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(result.getIsSystem()).isFalse();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    // ============ updateTemplate ============

    @Test
    @DisplayName("updateTemplate：所有者更新成功")
    void given_owner_when_updateTemplate_then_success() {
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(createTemplate());

        ReportTemplateService.UpdateTemplateRequest request = new ReportTemplateService.UpdateTemplateRequest();
        request.setName("新名称");

        ReportTemplate result = reportTemplateService.updateTemplate(TEMPLATE_ID, request);

        assertThat(result.getName()).isEqualTo("新名称");
        verify(reportTemplateMapper).update(any(ReportTemplate.class));
    }

    @Test
    @DisplayName("updateTemplate：非所有者且非 ADMIN 抛异常")
    void given_nonOwnerNonAdmin_when_updateTemplate_then_throw() {
        ReportTemplate template = createTemplate();
        template.setCreatedBy(999L);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
        UserContext.setRole("USER");

        ReportTemplateService.UpdateTemplateRequest request = new ReportTemplateService.UpdateTemplateRequest();
        request.setName("越权更新");

        assertThatThrownBy(() -> reportTemplateService.updateTemplate(TEMPLATE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_RESOURCE_OWNER.getCode());
    }

    @Test
    @DisplayName("updateTemplate：系统级模板不可更新")
    void given_systemTemplate_when_updateTemplate_then_throw() {
        ReportTemplate template = createTemplate();
        template.setIsSystem(true);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
        UserContext.setRole("ADMIN");

        ReportTemplateService.UpdateTemplateRequest request = new ReportTemplateService.UpdateTemplateRequest();
        request.setName("更新系统模板");

        assertThatThrownBy(() -> reportTemplateService.updateTemplate(TEMPLATE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN.getCode());
    }

    // ============ deleteTemplate ============

    @Test
    @DisplayName("deleteTemplate：创建人删除自己模板成功")
    void given_owner_when_deleteTemplate_then_success() {
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(createTemplate());

        reportTemplateService.deleteTemplate(TEMPLATE_ID);

        verify(reportTemplateMapper).deleteById(TEMPLATE_ID);
    }

    @Test
    @DisplayName("deleteTemplate：系统级模板不可删除")
    void given_systemTemplate_when_deleteTemplate_then_throw() {
        ReportTemplate template = createTemplate();
        template.setIsSystem(true);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
        UserContext.setRole("ADMIN");

        assertThatThrownBy(() -> reportTemplateService.deleteTemplate(TEMPLATE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN.getCode());
    }

    // ============ getTemplate ============

    @Test
    @DisplayName("getTemplate：本租户模板可见")
    void given_ownTenantTemplate_when_getTemplate_then_return() {
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(createTemplate());

        ReportTemplate result = reportTemplateService.getTemplate(TEMPLATE_ID);

        assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    @DisplayName("getTemplate：系统级模板可见")
    void given_systemTemplate_when_getTemplate_then_return() {
        ReportTemplate template = createTemplate();
        template.setTenantId(0L);
        template.setIsSystem(true);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

        ReportTemplate result = reportTemplateService.getTemplate(TEMPLATE_ID);

        assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    @DisplayName("getTemplate：其他租户公开模板可见")
    void given_publicTemplateOfOtherTenant_when_getTemplate_then_return() {
        ReportTemplate template = createTemplate();
        template.setTenantId(200L);
        template.setIsPublic(true);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

        ReportTemplate result = reportTemplateService.getTemplate(TEMPLATE_ID);

        assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    @DisplayName("getTemplate：其他租户私有模板不可见")
    void given_privateTemplateOfOtherTenant_when_getTemplate_then_throw() {
        ReportTemplate template = createTemplate();
        template.setTenantId(200L);
        template.setIsPublic(false);
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);

        assertThatThrownBy(() -> reportTemplateService.getTemplate(TEMPLATE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REPORT_TEMPLATE_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("getTemplate：不存在抛异常")
    void given_nonExistent_when_getTemplate_then_throw() {
        when(reportTemplateMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> reportTemplateService.getTemplate(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REPORT_TEMPLATE_NOT_FOUND.getCode());
    }

    // ============ listTemplates ============

    @Test
    @DisplayName("listTemplates：本租户模板分页查询")
    void given_pagination_when_listTemplates_then_returnPage() {
        when(reportTemplateMapper.selectList(eq(TENANT_ID), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createTemplate()));
        when(reportTemplateMapper.countList(eq(TENANT_ID), any(), any(), any())).thenReturn(1L);

        PageResult<ReportTemplate> result = reportTemplateService.listTemplates(null, null, false, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("listTemplates：包含公开/系统模板")
    void given_includePublic_when_listTemplates_then_queryPublic() {
        when(reportTemplateMapper.selectPublicTemplates(eq(TENANT_ID), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createTemplate()));
        when(reportTemplateMapper.countPublicTemplates(eq(TENANT_ID), any(), any(), any())).thenReturn(1L);

        PageResult<ReportTemplate> result = reportTemplateService.listTemplates(null, null, true, 1, 20);

        assertThat(result.getList()).hasSize(1);
        verify(reportTemplateMapper, never()).selectList(any(), any(), any(), any(), anyInt(), anyInt());
    }

    // ============ applyTemplate ============

    @Test
    @DisplayName("applyTemplate：基于模板创建报表")
    void given_template_when_applyTemplate_then_createReport() {
        ReportTemplate template = createTemplate();
        when(reportTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
        when(reportService.createReport(any(ReportService.CreateReportRequest.class))).thenAnswer(invocation -> {
            Report report = new Report();
            report.setId(REPORT_ID);
            report.setName(template.getName());
            report.setTemplateConfig(template.getLayoutJson());
            return report;
        });

        ReportTemplateService.ApplyTemplateRequest request = new ReportTemplateService.ApplyTemplateRequest();
        request.setName("应用后的报表");

        Long result = reportTemplateService.applyTemplate(TEMPLATE_ID, request);

        assertThat(result).isEqualTo(REPORT_ID);
        verify(reportService).createReport(argThat(r ->
                "应用后的报表".equals(r.getName()) && template.getLayoutJson().equals(r.getTemplateConfig())));
    }
}
