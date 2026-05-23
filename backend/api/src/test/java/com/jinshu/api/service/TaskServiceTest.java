package com.jinshu.api.service;

import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
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

@DisplayName("TaskService 任务调度服务测试")
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    private TaskService taskService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long TASK_ID = 500L;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskMapper);
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

    private Task createDefaultTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setTaskType("IMPORT");
        task.setStatus("PENDING");
        task.setPriority(1);
        task.setProgress(0);
        task.setCreatedBy(USER_ID);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    @Test
    @DisplayName("getTaskById：存在且同租户，返回任务")
    void given_existingTask_when_getById_then_return() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(createDefaultTask());

        Task result = taskService.getTaskById(TASK_ID);

        assertThat(result.getId()).isEqualTo(TASK_ID);
        assertThat(result.getTaskType()).isEqualTo("IMPORT");
    }

    @Test
    @DisplayName("getTaskById：跨租户访问抛异常")
    void given_differentTenantTask_when_getById_then_throw() {
        Task otherTenant = createDefaultTask();
        otherTenant.setTenantId(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(otherTenant);

        assertThatThrownBy(() -> taskService.getTaskById(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    @DisplayName("getTaskById：不存在抛异常")
    void given_nonExistentTask_when_getById_then_throw() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TASK_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("listTasks：分页查询成功")
    void given_pagination_when_listTasks_then_returnPage() {
        when(taskMapper.selectList(eq(TENANT_ID), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createDefaultTask()));
        when(taskMapper.countList(eq(TENANT_ID), any())).thenReturn(1L);

        PageResult<Task> result = taskService.listTasks(null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("cancelTask：创建人可取消 PENDING 任务")
    void given_owner_when_cancelPendingTask_then_cancelled() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(createDefaultTask());

        taskService.cancelTask(TASK_ID);

        verify(taskMapper).update(argThat(t -> "CANCELLED".equals(t.getStatus())));
    }

    @Test
    @DisplayName("cancelTask：非创建者抛异常")
    void given_nonOwner_when_cancelTask_then_throw() {
        Task task = createDefaultTask();
        task.setCreatedBy(999L);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> taskService.cancelTask(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权取消");
    }

    @Test
    @DisplayName("cancelTask：已完成任务不可取消")
    void given_completedTask_when_cancel_then_throw() {
        Task completed = createDefaultTask();
        completed.setStatus("COMPLETED");
        when(taskMapper.selectById(TASK_ID)).thenReturn(completed);

        assertThatThrownBy(() -> taskService.cancelTask(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PARAM_ERROR.getCode());
    }
}
