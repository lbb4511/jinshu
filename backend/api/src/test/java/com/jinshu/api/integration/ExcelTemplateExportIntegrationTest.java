package com.jinshu.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.ExcelTemplateFileMapper;
import com.jinshu.api.service.ExportTaskService;
import com.jinshu.api.service.TemplateFileService;
import com.jinshu.common.entity.ExcelTemplateFile;
import com.jinshu.common.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 模板上传与导出任务集成测试。
 * 验证上传模板后，可在创建导出任务时携带 templateId，且任务参数中正确记录。
 */
@Transactional
@DisplayName("Excel 模板导出 - 集成测试")
class ExcelTemplateExportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TemplateFileService templateFileService;

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private ExcelTemplateFileMapper excelTemplateFileMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Path tempBaseDir;

    @BeforeEach
    void setUpFileBasePath() throws Exception {
        tempBaseDir = Files.createTempDirectory("jinshu-integration-test");
        System.setProperty("jinshu.file.base-path", tempBaseDir.toString());
    }

    @Test
    @DisplayName("上传模板并创建带 templateId 的导出任务")
    void given_uploadedTemplate_when_createExportTask_then_templateIdInConfig() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake-excel-content".getBytes());

        ExcelTemplateFile template = templateFileService.uploadTemplate(null, file);
        assertThat(template.getId()).isNotNull();

        ExcelTemplateFile persisted = excelTemplateFileMapper.selectById(template.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(Path.of(persisted.getFilePath())).exists();

        jdbcTemplate.update(
                "INSERT INTO meta.report_metadata (tenant_id, name, status, schema_version, created_at, updated_at) VALUES (?, ?, 'DRAFT', 1, NOW(), NOW())",
                TENANT_ID, "integration-report");
        Long reportId = jdbcTemplate.queryForObject(
                "SELECT id FROM meta.report_metadata WHERE tenant_id = ? AND name = ?",
                Long.class, TENANT_ID, "integration-report");

        ExportTaskService.ExportRequest request = new ExportTaskService.ExportRequest();
        request.setReportId(reportId);
        request.setFormat("EXCEL");
        request.setTemplateId(template.getId());

        Long taskId = exportTaskService.createExportTask(request);
        Task task = jdbcTemplate.queryForObject(
                "SELECT id, tenant_id, task_type, status, parameters FROM task.task WHERE id = ?",
                (rs, rowNum) -> {
                    Task t = new Task();
                    t.setId(rs.getLong("id"));
                    t.setTenantId(rs.getLong("tenant_id"));
                    t.setTaskType(rs.getString("task_type"));
                    t.setStatus(rs.getString("status"));
                    t.setParameters(rs.getString("parameters"));
                    return t;
                }, taskId);

        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo("EXPORT");
        assertThat(task.getStatus()).isEqualTo("PENDING");

        var config = objectMapper.readValue(task.getParameters(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertThat(config).containsKey("templateId");
        assertThat(config.get("templateId")).isEqualTo(template.getId().intValue());
        assertThat(config).containsKey("tenantId");
        assertThat(config).containsKey("outputPath");

        System.clearProperty("jinshu.file.base-path");
        if (tempBaseDir != null) {
            Files.walk(tempBaseDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}
