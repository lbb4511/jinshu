package com.jinshu.worker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${jinshu.export.queue:jinshu.export}")
public class ExportTaskConsumer {

    private final ExportService exportService;

    private final ObjectMapper objectMapper;

    @RabbitHandler
    public void handleExportTask(Map<String, Object> messageMap) {
        ExportMessage message = objectMapper.convertValue(messageMap, ExportMessage.class);
        Long taskId = message.getTaskId();
        Long tenantId = message.getTenantId();
        log.info("Received export task: taskId={}, tenantId={}", taskId, tenantId);

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            exportService.executeExport(taskId);
            log.info("Export task completed: taskId={}", taskId);
        } catch (Exception e) {
            log.error("Export task failed: taskId={}", taskId, e);
            exportService.markFailed(taskId, e.getMessage());
            throw new RuntimeException("Export processing failed", e);
        } finally {
            TenantContext.clear();
        }
    }

    public static class ExportMessage {
        private Long taskId;
        private Long tenantId;
        private String format;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
