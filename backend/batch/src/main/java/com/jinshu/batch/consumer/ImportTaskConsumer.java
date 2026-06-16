package com.jinshu.batch.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.batch.dao.ImportErrorLogMapper;
import com.jinshu.batch.dao.ReportDataMapper;
import com.jinshu.batch.model.*;
import com.jinshu.batch.processor.ReportRowValidator;
import com.jinshu.batch.reader.ExcelItemReader;
import com.jinshu.batch.writer.ImportErrorLogWriter;
import com.jinshu.batch.writer.ReportBatchWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RabbitListener(queues = "${jinshu.import.queue:P4}")
public class ImportTaskConsumer {

    private final StringRedisTemplate redisTemplate;

    private final ReportDataMapper reportDataMapper;

    private final ImportErrorLogMapper errorLogMapper;

    private final ObjectMapper objectMapper;

    public ImportTaskConsumer(StringRedisTemplate redisTemplate,
                              ReportDataMapper reportDataMapper,
                              ImportErrorLogMapper errorLogMapper,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.reportDataMapper = reportDataMapper;
        this.errorLogMapper = errorLogMapper;
        this.objectMapper = objectMapper;
    }

    @RabbitHandler
    public void handleImportTask(Map<String, Object> messageMap) {
        ImportTaskMessage message = objectMapper.convertValue(messageMap, ImportTaskMessage.class);
        Long taskId = message.getTaskId();
        log.info("Received import task: taskId={}, filePath={}", taskId, message.getFilePath());

        try {
            processImport(message);
        } catch (Exception e) {
            log.error("Import task failed: taskId={}", taskId, e);
            updateTaskStatus(taskId, "FAILED");
            throw new RuntimeException("Import processing failed", e);
        }
    }

    private void processImport(ImportTaskMessage message) {
        Long taskId = message.getTaskId();
        Long tenantId = message.getTenantId();
        Long reportId = message.getReportId();
        String filePath = message.getFilePath();
        List<String> columnHeaders = message.getColumnHeaders();
        List<ColumnSchema> columns = message.getColumns();
        List<BusinessRule> businessRules = message.getBusinessRules();
        int totalRows = message.getTotalRows();
        int shardSeq = message.getShardSeq();
        int shardSize = message.getShardSize();

        int shardStartRow = shardSeq * shardSize;
        int shardEndRow = Math.min(shardStartRow + shardSize, totalRows);

        updateTaskStatus(taskId, "PROCESSING");

        Set<String> uniqueCache = new HashSet<>();
        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        ReportBatchWriter dataWriter = new ReportBatchWriter(reportDataMapper, tenantId, reportId);
        ImportErrorLogWriter errorWriter = new ImportErrorLogWriter(errorLogMapper, tenantId, taskId, reportId);

        int[] totalProcessed = {0};
        int[] totalErrors = {0};

        ExcelItemReader reader = new ExcelItemReader(filePath, columnHeaders, shardStartRow, shardEndRow);

        reader.read(
                batch -> {
                    for (ExcelImportRow row : batch) {
                        ValidationResult result = validator.validate(row);

                        if (result.isValid()) {
                            dataWriter.write(row.getCells(), row.getRowNo());
                        } else {
                            errorWriter.writeErrors(result.getErrors());
                            totalErrors[0]++;
                        }
                        totalProcessed[0]++;
                    }
                    dataWriter.flush();
                    errorWriter.flush();
                },
                processed -> updateProgress(taskId, processed, totalErrors[0])
        );

        dataWriter.flush();
        errorWriter.flush();

        int errorRate = totalProcessed[0] > 0 ? (totalErrors[0] * 100 / totalProcessed[0]) : 0;
        if (errorRate > 30) {
            log.warn("Error rate {}% exceeds threshold, marking task as FAILED", errorRate);
            updateTaskStatus(taskId, "FAILED");
            return;
        }

        updateTaskStatus(taskId, "SUCCESS");
        updateProgress(taskId, totalProcessed[0], totalErrors[0]);
        log.info("Import shard completed: taskId={}, shard={}, rows={}, errors={}",
                taskId, shardSeq, totalProcessed[0], totalErrors[0]);
    }

    private void updateProgress(Long taskId, int processed, int errors) {
        String key = "jinshu:import:progress:" + taskId;
        redisTemplate.opsForHash().put(key, "processedRows", String.valueOf(processed));
        redisTemplate.opsForHash().put(key, "failedRows", String.valueOf(errors));
    }

    private void updateTaskStatus(Long taskId, String status) {
        String key = "jinshu:import:progress:" + taskId;
        redisTemplate.opsForHash().put(key, "status", status);
    }

    public static class ImportTaskMessage {

        private Long taskId;

        private String filePath;

        private Long reportId;

        private Long tenantId;

        private List<String> columnHeaders;

        private List<ColumnSchema> columns;

        private List<BusinessRule> businessRules;

        private int totalRows;

        private int shardSeq;

        private int shardSize;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public List<String> getColumnHeaders() { return columnHeaders; }
        public void setColumnHeaders(List<String> columnHeaders) { this.columnHeaders = columnHeaders; }
        public List<ColumnSchema> getColumns() { return columns; }
        public void setColumns(List<ColumnSchema> columns) { this.columns = columns; }
        public List<BusinessRule> getBusinessRules() { return businessRules; }
        public void setBusinessRules(List<BusinessRule> businessRules) { this.businessRules = businessRules; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getShardSeq() { return shardSeq; }
        public void setShardSeq(int shardSeq) { this.shardSeq = shardSeq; }
        public int getShardSize() { return shardSize; }
        public void setShardSize(int shardSize) { this.shardSize = shardSize; }
    }
}
