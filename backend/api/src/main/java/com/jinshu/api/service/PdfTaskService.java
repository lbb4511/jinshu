package com.jinshu.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.PdfRenderConfig;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.utils.FileNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTaskService {

    private static final int MAX_PAGES = 500;

    private final TaskMapper taskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;

    @Value("${jinshu.render.queue:jinshu.render}")
    private String renderQueue;

    @Transactional
    @AuditLog(operation = "PDF_GENERATE", targetType = "REPORT")
    public Long createPdfTask(PdfSubmitRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        var report = reportService.getReportById(request.getReportId());
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }

        String colorSpace = request.getColorSpace() != null ? request.getColorSpace() : "RGB";
        if (!"RGB".equals(colorSpace) && !"CMYK".equals(colorSpace)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的色彩空间: " + colorSpace);
        }

        String outputPath = FileNameUtil.generateExportFilePath(tenantId, request.getReportId(), "PDF");

        Map<String, Object> config = new HashMap<>();
        config.put("reportId", request.getReportId());
        config.put("colorSpace", colorSpace);
        config.put("watermarkEnabled", request.isWatermarkEnabled());
        config.put("outputPath", outputPath);
        config.put("pageCount", 0);
        config.put("segments", java.util.Collections.emptyList());
        config.put("fallback", false);

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType("PDF");
        task.setStatus("PENDING");
        task.setPriority(2);
        task.setReportId(request.getReportId());
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now());
        try {
            task.setParameters(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PDF config", e);
        }

        taskMapper.insert(task);

        rabbitTemplate.convertAndSend(renderQueue, Map.of(
            "taskId", task.getId(),
            "tenantId", tenantId
        ));

        log.info("PDF render task created: taskId={}, reportId={}", task.getId(), request.getReportId());
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
        result.put("status", task.getStatus());
        result.put("progress", task.getProgress());
        result.put("startedAt", task.getStartedAt());
        result.put("completedAt", task.getCompletedAt());
        result.put("errorMessage", task.getErrorMessage());

        if (task.getParameters() != null) {
            try {
                Map<String, Object> config = objectMapper.readValue(task.getParameters(), Map.class);
                result.put("segments", config.getOrDefault("segments", java.util.Collections.emptyList()));
                result.put("colorSpace", config.getOrDefault("colorSpace", "RGB"));
            } catch (Exception e) {
                log.warn("Failed to parse PDF config for task {}", taskId, e);
            }
        }
        return result;
    }

    public Map<String, Object> generateDownloadLink(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ERROR, "任务未完成，无法下载");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("downloadUrl", "/api/v1/files/download?taskId=" + taskId);
        result.put("expiresAt", LocalDateTime.now().plusHours(1).toString());
        result.put("fileName", "report_" + task.getReportId() + "_" + System.currentTimeMillis() + ".pdf");
        result.put("fileSize", "0MB");

        if (task.getParameters() != null) {
            try {
                Map<String, Object> config = objectMapper.readValue(task.getParameters(), Map.class);
                result.put("colorSpace", config.getOrDefault("colorSpace", "RGB"));
                result.put("warning", config.get("fallback"));
            } catch (Exception e) {
                log.warn("Failed to parse PDF config for download task {}", taskId, e);
            }
        }
        return result;
    }

    public Map<String, Object> estimate(Long reportId) {
        var report = reportService.getReportById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("maxPages", MAX_PAGES);
        result.put("supportedColorSpaces", java.util.List.of("RGB", "CMYK"));
        return result;
    }

    public static class PdfSubmitRequest {
        private Long reportId;
        private String colorSpace;
        private boolean watermarkEnabled;

        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public String getColorSpace() { return colorSpace; }
        public void setColorSpace(String colorSpace) { this.colorSpace = colorSpace; }
        public boolean isWatermarkEnabled() { return watermarkEnabled; }
        public void setWatermarkEnabled(boolean watermarkEnabled) { this.watermarkEnabled = watermarkEnabled; }
    }
}
