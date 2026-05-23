package com.jinshu.api.service;

import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    public Task getTaskById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此任务");
        }
        return task;
    }

    public PageResult<Task> listTasks(String status, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;
        List<Task> tasks = taskMapper.selectList(tenantId, status, offset, pageSize);
        long total = taskMapper.countList(tenantId, status);
        return PageResult.of(tasks, total, page, pageSize);
    }

    @Transactional
    @AuditLog(operation = "CANCEL_TASK", targetType = "TASK")
    public Task cancelTask(Long id) {
        Task task = getTaskById(id);
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();

        if (!task.getCreatedBy().equals(userId) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权取消此任务");
        }

        if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus()) || "CANCELLED".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已完成，无法取消");
        }

        task.setStatus("CANCELLED");
        task.setCancelledAt(LocalDateTime.now());
        taskMapper.update(task);

        return task;
    }

    public Map<String, Object> getQueueStatus() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> queues = new java.util.ArrayList<>();

        String[] priorities = {"P0", "P1", "P2", "P3", "P4"};
        for (String p : priorities) {
            Map<String, Object> queue = new HashMap<>();
            queue.put("priority", p);
            queue.put("pending", 0);
            queue.put("processing", 0);
            queues.add(queue);
        }

        result.put("queues", queues);
        result.put("totalPending", 0);
        result.put("totalProcessing", 0);
        result.put("activeTenants", 0);

        return result;
    }

    public Map<String, Object> getSchedulerStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "HEALTHY");
        result.put("activeWorkers", 0);
        result.put("currentLeader", "worker-1");
        result.put("lastHeartbeat", LocalDateTime.now().toString());
        return result;
    }

    public Map<String, Object> getTenantQuota(Long tenantId) {
        Map<String, Object> quota = new HashMap<>();
        quota.put("maxConcurrentTasks", 5);
        quota.put("maxExportRows", 1000000);
        quota.put("maxPdfPages", 500);
        quota.put("dailyTaskQuota", 100);
        quota.put("priorityBoost", 0);
        return quota;
    }

    public Map<String, Object> updateTenantQuota(Long tenantId, Map<String, Object> quota) {
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("quota", quota);
        result.put("effectiveImmediately", true);
        return result;
    }
}
