package com.jinshu.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.utils.FileNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskService {

    private static final int MAX_CONCURRENT_EXPORT = 2;

    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${jinshu.export.queue:jinshu.export}")
    private String exportQueue;

    public Map<String, Object> estimate(Long reportId, String dateFrom, String dateTo) {
        Long tenantId = TenantContext.getTenantId();
        var report = reportService.getReportById(reportId);

        long totalRows = 0;
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("totalRows", totalRows);
        result.put("estimatedFileSize", estimateSize(totalRows));
        result.put("estimatedTime", estimateTime(totalRows));
        result.put("suggestFormat", suggestFormat(totalRows));
        return result;
    }

    @Transactional
    @AuditLog(operation = "EXPORT_REPORT", targetType = "REPORT")
    public Long createExportTask(ExportRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        reportService.getReportById(request.getReportId());

        long activeCount = taskMapper.countByTypeAndStatus(tenantId, "EXPORT", "PROCESSING");
        if (activeCount >= MAX_CONCURRENT_EXPORT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前导出任务已满，请等待");
        }

        String format = request.getFormat() != null ? request.getFormat() : "EXCEL";
        String outputPath = FileNameUtil.generateExportFilePath(tenantId, request.getReportId(), format);

        Map<String, Object> config = new HashMap<>();
        config.put("reportId", request.getReportId());
        config.put("format", format);
        config.put("filters", request.getFilters());
        config.put("outputPath", outputPath);

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType("EXPORT");
        task.setStatus("PENDING");
        task.setPriority(1);
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now());
        try {
            task.setParameters(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize export config", e);
        }

        taskMapper.insert(task);

        Map<String, Object> message = new HashMap<>();
        message.put("taskId", task.getId());
        message.put("tenantId", tenantId);
        message.put("format", format);
        rabbitTemplate.convertAndSend(exportQueue, message);

        log.info("Export task created and queued: taskId={}, format={}", task.getId(), format);
        return task.getId();
    }

    public Map<String, Object> getTaskProgress(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);

        Map<Object, Object> progressEntries = getRedisProgress(taskId);
        if (progressEntries != null && !progressEntries.isEmpty()) {
            result.put("status", progressEntries.get("status"));
            result.put("progress", parseProgress(progressEntries.get("progress")));
            result.put("processedRows", parseProgress(progressEntries.get("processedRows")));
            result.put("totalRows", parseProgress(progressEntries.get("totalRows")));
            result.put("message", progressEntries.get("message"));
        } else {
            result.put("status", task.getStatus());
            result.put("progress", task.getProgress());
        }
        result.put("startedAt", task.getStartedAt());
        result.put("completedAt", task.getCompletedAt());
        return result;
    }

    private Map<Object, Object> getRedisProgress(Long taskId) {
        try {
            return redisTemplate.opsForHash().entries("jinshu:export:progress:" + taskId);
        } catch (Exception e) {
            log.warn("Failed to read export progress from Redis for task {}: {}", taskId, e.getMessage());
            return null;
        }
    }

    private Integer parseProgress(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Map<String, Object> generateDownloadLink(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务未完成，无法下载");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("downloadUrl", "/api/v1/files/download?taskId=" + taskId);
        result.put("expiresAt", LocalDateTime.now().plusHours(1).toString());
        result.put("fileSize", resolveFileSizeText(task));
        return result;
    }

    private String resolveFileSizeText(Task task) {
        Long size = task.getResultFileSize();
        if (size == null || size <= 0) {
            return "0MB";
        }
        if (size < 1024) {
            return size + "B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        }
        if (size < 1024L * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024));
        }
        return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
    }

    private String estimateSize(long rows) {
        if (rows < 10000) return "0.5MB";
        if (rows < 100000) return "2MB";
        if (rows < 500000) return "10MB";
        return "50MB+";
    }

    private String estimateTime(long rows) {
        if (rows < 10000) return "< 5s";
        if (rows < 100000) return "< 30s";
        if (rows < 500000) return "< 2m";
        return "< 5m";
    }

    private String suggestFormat(long rows) {
        if (rows > 1000000) return "CSV";
        return "EXCEL";
    }

    public static class ExportRequest {
        private Long reportId;
        private String format;
        private Map<String, Object> filters;

        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    }
}
