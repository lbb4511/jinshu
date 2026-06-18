package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ExportTaskService 导出任务服务测试")
@ExtendWith(MockitoExtension.class)
class ExportTaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ReportService reportService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ExportTaskService exportTaskService;

    private ObjectMapper objectMapper;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long REPORT_ID = 200L;
    private static final Long TASK_ID = 300L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exportTaskService = new ExportTaskService(taskMapper, objectMapper, reportService, rabbitTemplate);
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

    @Test
    @DisplayName("estimate: 返回预估信息包含行数和推荐格式")
    void given_reportId_when_estimate_then_returnEstimate() {
        Report report = new Report();
        report.setId(REPORT_ID);
        report.setTenantId(TENANT_ID);
        when(reportService.getReportById(REPORT_ID)).thenReturn(report);

        Map<String, Object> result = exportTaskService.estimate(REPORT_ID, null, null);

        assertThat(result).containsKey("reportId");
        assertThat(result).containsKey("totalRows");
        assertThat(result).containsKey("suggestFormat");
        assertThat(result.get("reportId")).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("createExportTask: 成功创建导出任务返回 taskId")
    void given_validRequest_when_createExportTask_then_returnTaskId() {
        when(reportService.getReportById(REPORT_ID)).thenReturn(new Report());
        when(taskMapper.countByTypeAndStatus(TENANT_ID, "EXPORT", "PROCESSING")).thenReturn(0L);
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return null;
        }).when(taskMapper).insert(any(Task.class));

        ExportTaskService.ExportRequest request = new ExportTaskService.ExportRequest();
        request.setReportId(REPORT_ID);
        request.setFormat("EXCEL");

        Long taskId = exportTaskService.createExportTask(request);

        assertThat(taskId).isEqualTo(TASK_ID);
        verify(taskMapper).insert(argThat(t ->
                "EXPORT".equals(t.getTaskType()) && "PENDING".equals(t.getStatus())));
    }

    @Test
    @DisplayName("createExportTask: 并发超限抛异常")
    void given_concurrentExceedsLimit_when_createExportTask_then_throw() {
        when(reportService.getReportById(REPORT_ID)).thenReturn(new Report());
        when(taskMapper.countByTypeAndStatus(TENANT_ID, "EXPORT", "PROCESSING")).thenReturn(2L);

        ExportTaskService.ExportRequest request = new ExportTaskService.ExportRequest();
        request.setReportId(REPORT_ID);

        assertThatThrownBy(() -> exportTaskService.createExportTask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("导出任务已满");
    }

    @Test
    @DisplayName("getTaskProgress: 返回任务进度信息")
    void given_existingTask_when_getProgress_then_returnProgress() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        task.setProgress(50);
        task.setStartedAt(LocalDateTime.now());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        Map<String, Object> result = exportTaskService.getTaskProgress(TASK_ID);

        assertThat(result.get("taskId")).isEqualTo(TASK_ID);
        assertThat(result.get("status")).isEqualTo("PROCESSING");
        assertThat(result.get("progress")).isEqualTo(50);
    }

    @Test
    @DisplayName("getTaskProgress: 跨租户任务抛异常")
    void given_otherTenantTask_when_getProgress_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> exportTaskService.getTaskProgress(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    @DisplayName("generateDownloadLink: 任务未完成抛异常")
    void given_uncompletedTask_when_getDownloadLink_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> exportTaskService.generateDownloadLink(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("任务未完成");
    }

    @Test
    @DisplayName("generateDownloadLink: 完成任务返回下载链接")
    void given_completedTask_when_getDownloadLink_then_returnLink() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("SUCCESS");
        task.setStartedAt(LocalDateTime.now());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        Map<String, Object> result = exportTaskService.generateDownloadLink(TASK_ID);

        assertThat(result.get("downloadUrl")).asString().contains("/api/v1/files/download");
        assertThat(result.get("expiresAt")).isNotNull();
    }
}
