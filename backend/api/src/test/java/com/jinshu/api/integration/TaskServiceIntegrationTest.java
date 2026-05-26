package com.jinshu.api.integration;

import com.jinshu.api.dao.TaskMapper;
import com.jinshu.api.service.TaskService;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskService - DB integration tests")
class TaskServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Task createTask(String status, String type) {
        Task task = new Task();
        task.setTenantId(TENANT_ID);
        task.setTaskType(type);
        task.setStatus(status);
        task.setPriority(0);
        task.setProgress(0);
        task.setCreatedBy(USER_ID);
        task.setCreatedAt(java.time.LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    @Nested
    @DisplayName("Get task")
    @Transactional
    class Get {

        private Task saved;

        @BeforeEach
        void setUp() {
            saved = createTask("PENDING", "EXPORT");
        }

        @Test
        @DisplayName("Should get by id")
        void shouldGetById() {
            Task task = taskService.getTaskById(saved.getId());
            assertThat(task.getStatus()).isEqualTo("PENDING");
            assertThat(task.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should throw cross-tenant access")
        void shouldThrowCrossTenant() {
            jdbcTemplate.update(
                "INSERT INTO task (tenant_id, task_type, status, priority, created_by, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                999L, "EXPORT", "PENDING", 0, USER_ID
            );
            Long otherId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM task WHERE tenant_id = 999", Long.class);

            assertThatThrownBy(() -> taskService.getTaskById(otherId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should throw when not found")
        void shouldThrowWhenNotFound() {
            assertThatThrownBy(() -> taskService.getTaskById(99999L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("List tasks")
    @Transactional
    class ListTasks {

        @BeforeEach
        void setUp() {
            for (int i = 0; i < 4; i++) {
                createTask(i < 2 ? "PENDING" : "COMPLETED", "EXPORT");
            }
        }

        @Test
        @DisplayName("Should list with pagination")
        void shouldListPaginated() {
            PageResult<Task> result = taskService.listTasks(null, 1, 2);
            assertThat(result.getList()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should filter by status")
        void shouldFilterByStatus() {
            PageResult<Task> result = taskService.listTasks("PENDING", 1, 10);
            assertThat(result.getList()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Cancel task")
    @Transactional
    class Cancel {

        @Test
        @DisplayName("Should cancel pending task")
        void shouldCancelPending() {
            Task task = createTask("PENDING", "EXPORT");

            Task cancelled = taskService.cancelTask(task.getId());
            assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
            assertThat(cancelled.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("Should cancel processing task")
        void shouldCancelProcessing() {
            Task task = createTask("PROCESSING", "PDF");

            Task cancelled = taskService.cancelTask(task.getId());
            assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should not cancel completed task")
        void shouldNotCancelCompleted() {
            Task task = createTask("COMPLETED", "EXPORT");
            assertThatThrownBy(() -> taskService.cancelTask(task.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should not cancel failed task")
        void shouldNotCancelFailed() {
            Task task = createTask("FAILED", "EXPORT");
            assertThatThrownBy(() -> taskService.cancelTask(task.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should not cancel already cancelled task")
        void shouldNotCancelCancelled() {
            Task task = createTask("CANCELLED", "EXPORT");
            assertThatThrownBy(() -> taskService.cancelTask(task.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should reject cancel by non-owner non-admin")
        void shouldRejectNonOwner() {
            Task task = createTask("PENDING", "EXPORT");
            UserContext.setUserId(999L);
            UserContext.setRole("USER");

            assertThatThrownBy(() -> taskService.cancelTask(task.getId()))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Queue status")
    @Transactional
    class QueueStatus {

        @Test
        @DisplayName("Should return queue status map")
        void shouldReturnQueueStatus() {
            Map<String, Object> status = taskService.getQueueStatus();
            assertThat(status).containsKeys("queues", "totalPending", "totalProcessing", "activeTenants");
        }

        @Test
        @DisplayName("Should return scheduler status")
        void shouldReturnSchedulerStatus() {
            Map<String, Object> status = taskService.getSchedulerStatus();
            assertThat(status).containsKey("status");
            assertThat(status.get("status")).isEqualTo("HEALTHY");
        }
    }

    @Nested
    @DisplayName("Tenant quota")
    @Transactional
    class TenantQuota {

        @Test
        @DisplayName("Should return tenant quota")
        void shouldGetQuota() {
            Map<String, Object> quota = taskService.getTenantQuota(TENANT_ID);
            assertThat(quota).containsKeys("maxConcurrentTasks", "maxExportRows", "maxPdfPages", "dailyTaskQuota");
        }

        @Test
        @DisplayName("Should update tenant quota")
        void shouldUpdateQuota() {
            Map<String, Object> newQuota = Map.of("maxConcurrentTasks", 10);
            Map<String, Object> result = taskService.updateTenantQuota(TENANT_ID, newQuota);
            assertThat(result.get("tenantId")).isEqualTo(TENANT_ID);
            assertThat(result.get("effectiveImmediately")).isEqualTo(true);
        }
    }
}
