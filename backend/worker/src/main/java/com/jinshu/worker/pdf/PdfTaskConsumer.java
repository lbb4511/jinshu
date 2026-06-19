package com.jinshu.worker.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${jinshu.render.queue:jinshu.render}")
public class PdfTaskConsumer {

    private final PdfRenderOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final BusinessMetrics businessMetrics;

    @RabbitHandler
    public void handlePdfTask(Map<String, Object> messageMap) {
        PdfRenderMessage message = objectMapper.convertValue(messageMap, PdfRenderMessage.class);
        Long taskId = message.getTaskId();
        Long tenantId = message.getTenantId();
        log.info("Received PDF render task: taskId={}", taskId);

        businessMetrics.trackActiveTask("PDF", "PROCESSING", tenantId, 1);
        try {
            orchestrator.execute(taskId);
            businessMetrics.recordPdf("SUCCESS", tenantId);
            log.info("PDF render task completed: taskId={}", taskId);
        } catch (Exception e) {
            log.error("PDF render task failed: taskId={}", taskId, e);
            businessMetrics.recordPdf("FAILED", tenantId);
            orchestrator.markFailed(taskId, e.getMessage());
            throw new RuntimeException("PDF render processing failed", e);
        } finally {
            businessMetrics.trackActiveTask("PDF", "PROCESSING", tenantId, -1);
        }
    }

    public static class PdfRenderMessage {
        private Long taskId;
        private Long tenantId;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }
}
