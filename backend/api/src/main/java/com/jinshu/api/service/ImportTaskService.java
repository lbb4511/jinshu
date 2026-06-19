package com.jinshu.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.constant.ImportProgressKey;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportTaskService {

    private static final int MAX_CONCURRENT_IMPORT = 3;

    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${jinshu.import.queue:P4}")
    private String importQueue;

    @Transactional
    @AuditLog(operation = "IMPORT_REPORT", targetType = "REPORT")
    public Long createImportTask(ImportRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        long activeCount = taskMapper.countByTypeAndStatus(tenantId, "IMPORT", "PROCESSING");
        if (activeCount >= MAX_CONCURRENT_IMPORT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前导入任务已满，请等待");
        }

        int shardSize = request.getShardSize() > 0 ? request.getShardSize() : 10000;
        int totalShards = (request.getTotalRows() + shardSize - 1) / shardSize;

        Map<String, Object> config = new HashMap<>();
        config.put("reportId", request.getReportId());
        config.put("filePath", request.getFilePath());
        config.put("columnHeaders", request.getColumnHeaders());
        config.put("totalRows", request.getTotalRows());
        config.put("shardSize", shardSize);

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType("IMPORT");
        task.setStatus("PENDING");
        task.setPriority(1);
        task.setReportId(request.getReportId());
        task.setProgress(0);
        task.setShardTotal(totalShards);
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now());
        try {
            task.setParameters(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize import config", e);
        }

        taskMapper.insert(task);

        initProgress(task.getId(), totalShards, request.getTotalRows());

        for (int shardSeq = 0; shardSeq < totalShards; shardSeq++) {
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", task.getId());
            message.put("tenantId", tenantId);
            message.put("reportId", request.getReportId());
            message.put("filePath", request.getFilePath());
            message.put("columnHeaders", request.getColumnHeaders());
            message.put("totalRows", request.getTotalRows());
            message.put("shardSeq", shardSeq);
            message.put("shardSize", shardSize);

            rabbitTemplate.convertAndSend(importQueue, message);
        }

        log.info("Import task created and queued: taskId={}, reportId={}, shards={}",
            task.getId(), request.getReportId(), totalShards);
        return task.getId();
    }

    private void initProgress(Long taskId, int totalShards, int totalRows) {
        String key = ImportProgressKey.key(taskId);
        Map<String, String> fields = new HashMap<>();
        fields.put(ImportProgressKey.TOTAL_SHARDS, String.valueOf(totalShards));
        fields.put(ImportProgressKey.TOTAL_ROWS, String.valueOf(totalRows));
        redisTemplate.opsForHash().putAll(key, fields);
    }

    public Map<String, Object> getTaskProgress(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        String key = ImportProgressKey.key(taskId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Integer totalShards = parseInt(entries.get(ImportProgressKey.TOTAL_SHARDS));
        if ((totalShards == null || totalShards <= 0) && task.getShardTotal() != null && task.getShardTotal() > 0) {
            totalShards = task.getShardTotal();
        }
        if (totalShards == null || totalShards <= 0) {
            totalShards = 0;
        }

        Integer totalRows = parseInt(entries.get(ImportProgressKey.TOTAL_ROWS));
        if ((totalRows == null || totalRows <= 0) && task.getParameters() != null) {
            totalRows = parseTotalRowsFromParameters(task.getParameters());
        }
        if (totalRows == null || totalRows <= 0) {
            totalRows = 0;
        }

        int processedRows = 0;
        int failedRows = 0;
        boolean anyProcessing = false;
        boolean anyFailed = false;
        boolean hasShardData = false;
        boolean allSuccess = true;

        List<Map<String, Object>> shards = new ArrayList<>();
        for (int shardSeq = 0; shardSeq < totalShards; shardSeq++) {
            String processedField = ImportProgressKey.processedRows(shardSeq);
            String failedField = ImportProgressKey.failedRows(shardSeq);
            String statusField = ImportProgressKey.status(shardSeq);

            Integer shardProcessed = parseInt(entries.get(processedField));
            Integer shardFailed = parseInt(entries.get(failedField));
            String shardStatus = (String) entries.get(statusField);

            if (shardProcessed != null || shardFailed != null || shardStatus != null) {
                hasShardData = true;
            }

            int processed = shardProcessed == null ? 0 : shardProcessed;
            int failed = shardFailed == null ? 0 : shardFailed;
            processedRows += processed;
            failedRows += failed;

            if ("PROCESSING".equals(shardStatus)) {
                anyProcessing = true;
            } else if ("FAILED".equals(shardStatus)) {
                anyFailed = true;
            } else if (!"SUCCESS".equals(shardStatus)) {
                allSuccess = false;
            }

            Map<String, Object> shardInfo = new HashMap<>();
            shardInfo.put("shardSeq", shardSeq);
            shardInfo.put("processedRows", processed);
            shardInfo.put("failedRows", failed);
            shardInfo.put("status", shardStatus == null ? "PENDING" : shardStatus);
            shards.add(shardInfo);
        }

        String aggregatedStatus;
        if (anyProcessing) {
            aggregatedStatus = "PROCESSING";
        } else if (anyFailed) {
            aggregatedStatus = "FAILED";
        } else if (hasShardData && allSuccess) {
            aggregatedStatus = "SUCCESS";
        } else {
            aggregatedStatus = task.getStatus();
        }

        int progress;
        if (!hasShardData && task.getProgress() != null) {
            // Redis 尚无分片进度，回退到数据库状态
            progress = task.getProgress();
        } else if (totalRows > 0) {
            progress = (int) ((processedRows * 100L) / totalRows);
        } else if (task.getProgress() != null) {
            progress = task.getProgress();
        } else {
            progress = 0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", aggregatedStatus);
        result.put("processedRows", processedRows);
        result.put("failedRows", failedRows);
        result.put("totalRows", totalRows > 0 ? totalRows : null);
        result.put("progress", progress);
        result.put("totalShards", totalShards);
        result.put("shards", shards);
        result.put("startedAt", task.getStartedAt());
        result.put("completedAt", task.getCompletedAt());
        return result;
    }

    @Transactional
    @AuditLog(operation = "RETRY_IMPORT", targetType = "TASK")
    public Long retryImportTask(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        Task originalTask = taskMapper.selectById(taskId);
        if (originalTask == null || !tenantId.equals(originalTask.getTenantId())) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!"IMPORT".equals(originalTask.getTaskType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入任务重试");
        }
        if (originalTask.getParameters() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务参数缺失，无法重试");
        }

        Map<String, Object> config;
        try {
            config = objectMapper.readValue(originalTask.getParameters(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务参数解析失败");
        }

        Long reportId = parseLong(config.get("reportId"));
        String filePath = config.get("filePath") != null ? config.get("filePath").toString() : null;
        @SuppressWarnings("unchecked")
        List<String> columnHeaders = config.get("columnHeaders") instanceof List
                ? (List<String>) config.get("columnHeaders") : null;
        Integer totalRows = parseInt(config.get("totalRows"));
        Integer shardSize = parseInt(config.get("shardSize"));

        if (reportId == null || filePath == null || columnHeaders == null
                || totalRows == null || totalRows <= 0 || shardSize == null || shardSize <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务参数不完整，无法重试");
        }

        int effectiveShardSize = shardSize;
        int totalShards = (totalRows + effectiveShardSize - 1) / effectiveShardSize;

        Task retryTask = new Task();
        retryTask.setTenantId(tenantId);
        retryTask.setTaskType("IMPORT");
        retryTask.setStatus("PENDING");
        retryTask.setPriority(1);
        retryTask.setReportId(reportId);
        retryTask.setProgress(0);
        retryTask.setShardTotal(totalShards);
        retryTask.setParentTaskId(originalTask.getId());
        retryTask.setParameters(originalTask.getParameters());
        retryTask.setCreatedBy(userId);
        retryTask.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(retryTask);

        initProgress(retryTask.getId(), totalShards, totalRows);

        for (int shardSeq = 0; shardSeq < totalShards; shardSeq++) {
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", retryTask.getId());
            message.put("tenantId", tenantId);
            message.put("reportId", reportId);
            message.put("filePath", filePath);
            message.put("columnHeaders", columnHeaders);
            message.put("totalRows", totalRows);
            message.put("shardSeq", shardSeq);
            message.put("shardSize", effectiveShardSize);

            rabbitTemplate.convertAndSend(importQueue, message);
        }

        log.info("Import retry task created and queued: originalTaskId={}, retryTaskId={}, reportId={}, shards={}",
                taskId, retryTask.getId(), reportId, totalShards);
        return retryTask.getId();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseTotalRowsFromParameters(String parameters) {
        try {
            JsonNode node = objectMapper.readTree(parameters);
            JsonNode totalRowsNode = node.get("totalRows");
            if (totalRowsNode != null && totalRowsNode.isInt()) {
                return totalRowsNode.asInt();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse task parameters for totalRows", e);
        }
        return 0;
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class ImportRequest {
        private Long reportId;
        private String filePath;
        private List<String> columnHeaders;
        private int totalRows;
        private int shardSize;

        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public List<String> getColumnHeaders() { return columnHeaders; }
        public void setColumnHeaders(List<String> columnHeaders) { this.columnHeaders = columnHeaders; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getShardSize() { return shardSize; }
        public void setShardSize(int shardSize) { this.shardSize = shardSize; }
    }
}
