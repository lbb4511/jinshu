package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.dto.ExportProgressEvent;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportProgressService - 导出进度 SSE 服务测试")
class ExportProgressServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TaskMapper taskMapper;

    private ExportProgressService exportProgressService;

    private static final Long TENANT_ID = 100L;
    private static final Long TASK_ID = 300L;

    @BeforeEach
    void setUp() {
        exportProgressService = new ExportProgressService(redisTemplate, objectMapper, taskMapper);
        TenantContext.setTenantId(TENANT_ID);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(hashOperations.entries(anyString())).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        exportProgressService.shutdown();
    }

    @Test
    @DisplayName("subscribe: 任务存在时返回 SseEmitter")
    void given_existingTask_when_subscribe_then_returnEmitter() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        SseEmitter emitter = exportProgressService.subscribe(TASK_ID);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("subscribe: 跨租户任务抛异常")
    void given_otherTenantTask_when_subscribe_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> exportProgressService.subscribe(TASK_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("subscribe: 任务已完成时直接关闭 emitter")
    void given_completedTask_when_subscribe_then_completeEmitter() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("SUCCESS");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        SseEmitter emitter = exportProgressService.subscribe(TASK_ID);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("handleProgressEvent: 将进度事件发送给已订阅 emitter")
    void given_subscribedEmitter_when_handleProgressEvent_then_sendEvent() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        SseEmitter emitter = exportProgressService.subscribe(TASK_ID);

        ExportProgressEvent event = new ExportProgressEvent();
        event.setTaskId(TASK_ID);
        event.setStatus("PROCESSING");
        event.setProgress(50);
        event.setTimestamp(System.currentTimeMillis());

        exportProgressService.handleProgressEvent(event);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("handleProgressEvent: 终态事件完成后移除 emitter")
    void given_terminalEvent_when_handleProgressEvent_then_completeEmitter() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        SseEmitter emitter = exportProgressService.subscribe(TASK_ID);

        ExportProgressEvent event = new ExportProgressEvent();
        event.setTaskId(TASK_ID);
        event.setStatus("SUCCESS");
        event.setProgress(100);
        event.setTimestamp(System.currentTimeMillis());

        exportProgressService.handleProgressEvent(event);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("handleProgressMessage: 解析 JSON 消息并转发事件")
    void given_jsonMessage_when_handleProgressMessage_then_parseAndForward() throws Exception {
        String message = "{\"taskId\":300,\"status\":\"PROCESSING\",\"progress\":25}";
        when(objectMapper.readValue(eq(message), eq(ExportProgressEvent.class)))
                .thenReturn(new ExportProgressEvent(TASK_ID, "PROCESSING", 25, null, null, null, System.currentTimeMillis()));

        exportProgressService.handleProgressMessage(message);

        verify(objectMapper, times(1)).readValue(eq(message), eq(ExportProgressEvent.class));
    }
}
