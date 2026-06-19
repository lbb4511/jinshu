package com.jinshu.worker.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.mq.MqConstants;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 死信队列消费者：将最终失败的消息持久化到 task.mq_dead_letter，
 * 并更新对应任务状态为 FAILED，避免任务静默丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = MqConstants.DLQ_QUEUE, ackMode = "MANUAL")
    public void handleDeadLetter(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Map<String, Object> payload = parsePayload(message);
            Long taskId = parseLong(payload.get("taskId"));
            String msgId = parseString(payload.get("msgId"));
            String sourceQueue = extractSourceQueue(message);
            String messageType = inferMessageType(message, sourceQueue);
            String errorMessage = buildErrorMessage(message, sourceQueue);
            int deathCount = extractDeathCount(message);

            persistDeadLetter(msgId, taskId, messageType, sourceQueue, errorMessage, deathCount, payload);

            if (taskId != null) {
                markTaskFailed(taskId, errorMessage);
                updateProgressStatus(taskId, "FAILED");
            }

            channel.basicAck(deliveryTag, false);
            log.info("Dead letter processed: msgId={}, taskId={}, sourceQueue={}, deathCount={}",
                    msgId, taskId, sourceQueue, deathCount);
        } catch (Exception e) {
            log.error("Failed to process dead letter message, ack to avoid loop", e);
            channel.basicAck(deliveryTag, false);
        }
    }

    private Map<String, Object> parsePayload(Message message) throws IOException {
        return objectMapper.readValue(message.getBody(), new TypeReference<>() {});
    }

    private void persistDeadLetter(String msgId, Long taskId, String messageType, String sourceQueue,
                                   String errorMessage, int deathCount, Map<String, Object> payload)
            throws JsonProcessingException {
        String payloadJson = objectMapper.writeValueAsString(payload);
        jdbcTemplate.update(
                "INSERT INTO task.mq_dead_letter " +
                "(msg_id, task_id, message_type, source_queue, error_message, payload, x_death_count, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 'PROCESSED', ?)",
                msgId, taskId, messageType, sourceQueue, errorMessage,
                payloadJson, deathCount, LocalDateTime.now()
        );
    }

    private void markTaskFailed(Long taskId, String errorMessage) {
        int updated = jdbcTemplate.update(
                "UPDATE task.task SET status = 'FAILED', error_message = ?, completed_at = NOW() WHERE id = ?",
                errorMessage, taskId
        );
        if (updated == 0) {
            log.warn("No task found to mark failed: taskId={}", taskId);
        }
    }

    private void updateProgressStatus(Long taskId, String status) {
        try {
            HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
            ops.put("jinshu:import:progress:" + taskId, "status", status);
            ops.put("jinshu:export:progress:" + taskId, "status", status);
        } catch (Exception e) {
            log.warn("Failed to update Redis progress status for task {}: {}", taskId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractSourceQueue(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object queue = map.get("queue");
                if (queue != null) {
                    return queue.toString();
                }
            }
        }
        return message.getMessageProperties().getConsumerQueue();
    }

    @SuppressWarnings("unchecked")
    private int extractDeathCount(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object count = map.get("count");
                if (count instanceof Number number) {
                    return number.intValue();
                }
            }
        }
        return 1;
    }

    private String inferMessageType(Message message, String sourceQueue) {
        Object type = message.getMessageProperties().getHeaders().get("messageType");
        if (type != null) {
            return type.toString();
        }
        if (sourceQueue == null) {
            return "UNKNOWN";
        }
        if (sourceQueue.contains("import")) {
            return "IMPORT";
        }
        if (sourceQueue.contains("export")) {
            return "EXPORT";
        }
        if (sourceQueue.contains("render")) {
            return sourceQueue.contains("segment") ? "PDF_SEGMENT" : "PDF";
        }
        return "UNKNOWN";
    }

    private String buildErrorMessage(Message message, String sourceQueue) {
        Object reason = message.getMessageProperties().getHeaders().get("x-first-death-reason");
        Object exchange = message.getMessageProperties().getHeaders().get("x-first-death-exchange");
        StringBuilder sb = new StringBuilder();
        sb.append("Message moved to DLQ");
        if (reason != null) {
            sb.append(" (reason=").append(reason).append(")");
        }
        sb.append(" from queue=").append(sourceQueue);
        if (exchange != null) {
            sb.append(", exchange=").append(exchange);
        }
        return sb.toString();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseString(Object value) {
        return value == null ? null : value.toString();
    }
}
