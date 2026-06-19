package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.constant.ImportProgressKey;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ImportTaskService 导入任务服务测试")
@ExtendWith(MockitoExtension.class)
class ImportTaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ImportTaskService importTaskService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long REPORT_ID = 200L;
    private static final Long TASK_ID = 300L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        importTaskService = new ImportTaskService(taskMapper, objectMapper, rabbitTemplate, redisTemplate);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername("testuser");
        UserContext.setRole("USER");
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        ReflectionTestUtils.setField(importTaskService, "importQueue", "jinshu.import.task");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("createImportTask: 创建导入任务并初始化 Redis 分片进度")
    void given_validRequest_when_createImportTask_then_initShardProgress() {
        when(taskMapper.countByTypeAndStatus(TENANT_ID, "IMPORT", "PROCESSING")).thenReturn(0L);
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return null;
        }).when(taskMapper).insert(any(Task.class));

        ImportTaskService.ImportRequest request = new ImportTaskService.ImportRequest();
        request.setReportId(REPORT_ID);
        request.setFilePath("/data/file.xlsx");
        request.setColumnHeaders(List.of("col1", "col2"));
        request.setTotalRows(25000);
        request.setShardSize(10000);

        Long taskId = importTaskService.createImportTask(request);

        assertThat(taskId).isEqualTo(TASK_ID);

        verify(taskMapper).insert(argThat(t ->
                "IMPORT".equals(t.getTaskType())
                        && "PENDING".equals(t.getStatus())
                        && Integer.valueOf(0).equals(t.getProgress())
                        && Integer.valueOf(3).equals(t.getShardTotal())));

        String key = ImportProgressKey.key(TASK_ID);
        ArgumentCaptor<Map<String, String>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(key), fieldsCaptor.capture());
        Map<String, String> fields = fieldsCaptor.getValue();
        assertThat(fields).containsEntry(ImportProgressKey.TOTAL_SHARDS, "3");
        assertThat(fields).containsEntry(ImportProgressKey.TOTAL_ROWS, "25000");

        verify(rabbitTemplate, times(3)).convertAndSend(eq("jinshu.import.task"), anyMap());
    }

    @Test
    @DisplayName("createImportTask: 并发超限抛异常")
    void given_concurrentExceedsLimit_when_createImportTask_then_throw() {
        when(taskMapper.countByTypeAndStatus(TENANT_ID, "IMPORT", "PROCESSING")).thenReturn(3L);

        ImportTaskService.ImportRequest request = new ImportTaskService.ImportRequest();
        request.setReportId(REPORT_ID);

        assertThatThrownBy(() -> importTaskService.createImportTask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("导入任务已满");
    }

    @Test
    @DisplayName("getTaskProgress: 聚合 Redis 中所有分片进度")
    void given_shardedProgress_when_getTaskProgress_then_aggregate() {
        Task task = importTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        Map<Object, Object> entries = new HashMap<>();
        entries.put(ImportProgressKey.TOTAL_SHARDS, "3");
        entries.put(ImportProgressKey.TOTAL_ROWS, "100");
        entries.put(ImportProgressKey.processedRows(0), "30");
        entries.put(ImportProgressKey.failedRows(0), "2");
        entries.put(ImportProgressKey.status(0), "SUCCESS");
        entries.put(ImportProgressKey.processedRows(1), "50");
        entries.put(ImportProgressKey.failedRows(1), "1");
        entries.put(ImportProgressKey.status(1), "SUCCESS");
        entries.put(ImportProgressKey.processedRows(2), "20");
        entries.put(ImportProgressKey.failedRows(2), "0");
        entries.put(ImportProgressKey.status(2), "SUCCESS");
        when(hashOperations.entries(ImportProgressKey.key(TASK_ID))).thenReturn(entries);

        Map<String, Object> result = importTaskService.getTaskProgress(TASK_ID);

        assertThat(result.get("taskId")).isEqualTo(TASK_ID);
        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("processedRows")).isEqualTo(100);
        assertThat(result.get("failedRows")).isEqualTo(3);
        assertThat(result.get("totalRows")).isEqualTo(100);
        assertThat(result.get("progress")).isEqualTo(100);
        assertThat(result.get("totalShards")).isEqualTo(3);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shards = (List<Map<String, Object>>) result.get("shards");
        assertThat(shards).hasSize(3);
        assertThat(shards.get(1).get("processedRows")).isEqualTo(50);
    }

    @Test
    @DisplayName("getTaskProgress: 任一任务分片为 PROCESSING 时返回整体处理中")
    void given_oneShardProcessing_when_getTaskProgress_then_returnProcessing() {
        Task task = importTask();
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        Map<Object, Object> entries = new HashMap<>();
        entries.put(ImportProgressKey.TOTAL_SHARDS, "2");
        entries.put(ImportProgressKey.TOTAL_ROWS, "100");
        entries.put(ImportProgressKey.status(0), "SUCCESS");
        entries.put(ImportProgressKey.status(1), "PROCESSING");
        when(hashOperations.entries(ImportProgressKey.key(TASK_ID))).thenReturn(entries);

        Map<String, Object> result = importTaskService.getTaskProgress(TASK_ID);

        assertThat(result.get("status")).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("getTaskProgress: Redis 无数据时回退到数据库状态")
    void given_noRedisData_when_getTaskProgress_then_fallbackToDb() {
        Task task = importTask();
        task.setStatus("PENDING");
        task.setProgress(10);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(hashOperations.entries(ImportProgressKey.key(TASK_ID))).thenReturn(Map.of());

        Map<String, Object> result = importTaskService.getTaskProgress(TASK_ID);

        assertThat(result.get("status")).isEqualTo("PENDING");
        assertThat(result.get("progress")).isEqualTo(10);
    }

    @Test
    @DisplayName("getTaskProgress: 跨租户任务抛异常")
    void given_otherTenantTask_when_getTaskProgress_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> importTaskService.getTaskProgress(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    @DisplayName("retryImportTask: 成功创建重试任务并发送 MQ 消息")
    void given_failedImportTask_when_retry_then_createRetryTaskAndSendMessages() {
        String parameters = "{\"reportId\":200,\"filePath\":\"/data/file.xlsx\",\"columnHeaders\":[\"A\",\"B\"],\"totalRows\":15000,\"shardSize\":10000}";
        when(taskMapper.selectById(TASK_ID)).thenReturn(createFailedImportTask(parameters));
        doAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(999L);
            return null;
        }).when(taskMapper).insert(any(Task.class));

        Long retryTaskId = importTaskService.retryImportTask(TASK_ID);

        assertThat(retryTaskId).isEqualTo(999L);
        verify(taskMapper).insert(argThat(t -> "IMPORT".equals(t.getTaskType())
                && "PENDING".equals(t.getStatus())
                && TASK_ID.equals(t.getParentTaskId())));
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyMap());
    }

    @Test
    @DisplayName("retryImportTask: 非导入任务抛异常")
    void given_exportTask_when_retry_then_throw() {
        Task task = createFailedImportTask("{}");
        task.setTaskType("EXPORT");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> importTaskService.retryImportTask(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("retryImportTask: 跨租户任务抛异常")
    void given_crossTenantTask_when_retry_then_throw() {
        Task task = createFailedImportTask("{}");
        task.setTenantId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> importTaskService.retryImportTask(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TASK_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("retryImportTask: 参数不完整抛异常")
    void given_incompleteParameters_when_retry_then_throw() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(createFailedImportTask("{\"reportId\":200}"));

        assertThatThrownBy(() -> importTaskService.retryImportTask(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PARAM_ERROR.getCode());
    }

    private Task importTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setTaskType("IMPORT");
        task.setReportId(REPORT_ID);
        task.setStatus("PENDING");
        task.setShardTotal(3);
        task.setParameters("{\"totalRows\":100}");
        return task;
    }

    private Task createFailedImportTask(String parameters) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setTaskType("IMPORT");
        task.setReportId(REPORT_ID);
        task.setStatus("FAILED");
        task.setParameters(parameters);
        return task;
    }
}
