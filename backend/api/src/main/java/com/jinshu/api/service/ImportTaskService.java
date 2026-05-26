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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        Map<String, Object> config = new HashMap<>();
        config.put("reportId", request.getReportId());
        config.put("filePath", request.getFilePath());
        config.put("columnHeaders", request.getColumnHeaders());
        config.put("totalRows", request.getTotalRows());
        config.put("shardSize", request.getShardSize());

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType("IMPORT");
        task.setStatus("PENDING");
        task.setPriority(1);
        task.setReportId(request.getReportId());
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now());
        try {
            task.setParameters(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize import config", e);
        }

        taskMapper.insert(task);

        int shardSize = request.getShardSize() > 0 ? request.getShardSize() : 10000;
        int totalShards = (request.getTotalRows() + shardSize - 1) / shardSize;

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
        return result;
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
