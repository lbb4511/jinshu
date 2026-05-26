package com.jinshu.api.controller;

import com.jinshu.api.service.ExportTaskService;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportTaskService exportTaskService;

    @GetMapping("/estimate")
    public Result<Map<String, Object>> estimate(
            @RequestParam Long reportId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return Result.success(exportTaskService.estimate(reportId, dateFrom, dateTo));
    }

    @PostMapping
    public Result<Map<String, Object>> submitExport(@RequestBody ExportTaskService.ExportRequest request) {
        Long taskId = exportTaskService.createExportTask(request);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> getProgress(@PathVariable Long taskId) {
        return Result.success(exportTaskService.getTaskProgress(taskId));
    }

    @GetMapping("/{taskId}/download")
    public Result<Map<String, Object>> getDownloadLink(@PathVariable Long taskId) {
        return Result.success(exportTaskService.generateDownloadLink(taskId));
    }
}
