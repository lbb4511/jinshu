package com.jinshu.api.controller;

import com.jinshu.api.service.TaskService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.Task;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireRole;
import com.jinshu.common.security.SkipTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @RequireRole({"ADMIN", "USER"})
    public Result<PageResult<Task>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Task> result = taskService.listTasks(status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    public Result<Task> getTask(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return Result.success(task);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "CANCEL_TASK", targetType = "TASK")
    public Result<Task> cancelTask(@PathVariable Long id) {
        Task task = taskService.cancelTask(id);
        return Result.success(task);
    }

    @GetMapping("/queue/status")
    @SkipTenantFilter
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getQueueStatus() {
        Map<String, Object> result = taskService.getQueueStatus();
        return Result.success(result);
    }

    @GetMapping("/scheduler/status")
    @SkipTenantFilter
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> result = taskService.getSchedulerStatus();
        return Result.success(result);
    }

    @GetMapping("/tenant/{tenantId}/quota")
    @SkipTenantFilter
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getTenantQuota(@PathVariable Long tenantId) {
        Map<String, Object> result = taskService.getTenantQuota(tenantId);
        return Result.success(result);
    }

    @PutMapping("/tenant/{tenantId}/quota")
    @SkipTenantFilter
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> updateTenantQuota(@PathVariable Long tenantId, @RequestBody Map<String, Object> quota) {
        Map<String, Object> result = taskService.updateTenantQuota(tenantId, quota);
        return Result.success(result);
    }
}
