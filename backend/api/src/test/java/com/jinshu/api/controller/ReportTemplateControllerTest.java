package com.jinshu.api.controller;

import com.jinshu.api.service.ReportTemplateService;
import com.jinshu.common.entity.ReportTemplate;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportTemplateController 模板市场控制器测试")
@ExtendWith(MockitoExtension.class)
class ReportTemplateControllerTest {

    @Mock
    private ReportTemplateService reportTemplateService;

    private ReportTemplateController controller;

    private static final Long TEMPLATE_ID = 1L;
    private static final Long REPORT_ID = 100L;

    @BeforeEach
    void setUp() {
        controller = new ReportTemplateController(reportTemplateService);
    }

    @Test
    @DisplayName("POST /templates 创建模板")
    void given_validRequest_when_createTemplate_then_returnOk() {
        ReportTemplate template = new ReportTemplate();
        template.setId(TEMPLATE_ID);
        template.setName("销售模板");

        when(reportTemplateService.createTemplate(any(ReportTemplateService.CreateTemplateRequest.class)))
                .thenReturn(template);

        ReportTemplateService.CreateTemplateRequest request = new ReportTemplateService.CreateTemplateRequest();
        request.setName("销售模板");
        request.setLayoutJson("{\"version\":1}");

        Result<ReportTemplate> result = controller.createTemplate(request);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getId()).isEqualTo(TEMPLATE_ID);
        assertThat(result.getData().getName()).isEqualTo("销售模板");
    }

    @Test
    @DisplayName("GET /templates/{id} 查询模板")
    void given_existingTemplate_when_getTemplate_then_returnOk() {
        ReportTemplate template = new ReportTemplate();
        template.setId(TEMPLATE_ID);
        template.setName("销售模板");

        when(reportTemplateService.getTemplate(TEMPLATE_ID)).thenReturn(template);

        Result<ReportTemplate> result = controller.getTemplate(TEMPLATE_ID);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    @DisplayName("GET /templates 分页查询模板")
    void given_pagination_when_listTemplates_then_returnPage() {
        ReportTemplate template = new ReportTemplate();
        template.setId(TEMPLATE_ID);
        template.setName("销售模板");

        when(reportTemplateService.listTemplates(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(List.of(template), 1, 1, 20));

        Result<PageResult<ReportTemplate>> result = controller.listTemplates(null, null, null, 1, 20);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getList()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /templates/{id}/apply 应用模板")
    void given_template_when_applyTemplate_then_returnReportId() {
        when(reportTemplateService.applyTemplate(eq(TEMPLATE_ID), any(ReportTemplateService.ApplyTemplateRequest.class)))
                .thenReturn(REPORT_ID);

        ReportTemplateService.ApplyTemplateRequest request = new ReportTemplateService.ApplyTemplateRequest();
        request.setName("新报表");

        Result<Long> result = controller.applyTemplate(TEMPLATE_ID, request);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).isEqualTo(REPORT_ID);
    }
}
