package com.jinshu.api.controller;

import com.jinshu.api.service.ImportErrorLogService;
import com.jinshu.api.service.ImportTaskService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.ImportErrorLog;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/import-tasks")
@RequiredArgsConstructor
public class ImportTaskController {

    private final ImportErrorLogService importErrorLogService;
    private final ImportTaskService importTaskService;

    @PostMapping
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "IMPORT_REPORT", targetType = "REPORT")
    public Result<Map<String, Object>> createImportTask(@RequestBody ImportTaskService.ImportRequest request) {
        Long taskId = importTaskService.createImportTask(request);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/{taskId}/progress")
    @RequireRole({"ADMIN", "USER"})
    public Result<Map<String, Object>> getProgress(@PathVariable Long taskId) {
        return Result.success(importTaskService.getTaskProgress(taskId));
    }

    @GetMapping("/{taskId}/errors")
    @RequireRole({"ADMIN", "USER"})
    public Result<PageResult<ImportErrorLog>> listErrors(
            @PathVariable Long taskId,
            @RequestParam(required = false) Integer rowNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ImportErrorLog> result = importErrorLogService.listErrors(taskId, rowNo, page, pageSize);
        return Result.success(result);
    }

    @PostMapping("/{taskId}/retry")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "RETRY_IMPORT", targetType = "TASK")
    public Result<Map<String, Object>> retryImportTask(@PathVariable Long taskId) {
        Long retryTaskId = importTaskService.retryImportTask(taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("retryTaskId", retryTaskId);
        return Result.success(data);
    }
}
