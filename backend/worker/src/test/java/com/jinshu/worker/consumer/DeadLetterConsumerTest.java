package com.jinshu.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.mq.MqConstants;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeadLetterConsumer - 死信队列消费者")
class DeadLetterConsumerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private Channel channel;

    private ObjectMapper objectMapper;
    private DeadLetterConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new DeadLetterConsumer(jdbcTemplate, redisTemplate, objectMapper);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("处理包含 taskId 的死信消息：落库、更新任务状态、更新 Redis、ACK")
    void given_validDeadLetterWithTaskId_when_handle_then_persistAndAck() throws IOException {
        Map<String, Object> payload = Map.of(
                "msgId", "msg-001",
                "taskId", 42L,
                "reportId", 100L
        );
        Message message = buildMessage(payload, "jinshu.import.task", 3);

        consumer.handleDeadLetter(message, channel);

        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO task.mq_dead_letter " +
                   "(msg_id, task_id, message_type, source_queue, error_message, payload, x_death_count, status, created_at) " +
                   "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 'PROCESSED', ?)"),
                eq("msg-001"), eq(42L), eq("IMPORT"), eq("jinshu.import.task"),
                any(String.class), any(String.class), eq(3), any(java.time.LocalDateTime.class)
        );
        verify(jdbcTemplate, times(1)).update(
                eq("UPDATE task.task SET status = 'FAILED', error_message = ?, completed_at = NOW() WHERE id = ?"),
                any(String.class), eq(42L)
        );
        verify(hashOperations, times(1)).put("jinshu:import:progress:42", "status", "FAILED");
        verify(hashOperations, times(1)).put("jinshu:export:progress:42", "status", "FAILED");
        verify(channel, times(1)).basicAck(1L, false);
    }

    @Test
    @DisplayName("处理无 taskId 的死信消息：仅落库并 ACK")
    void given_deadLetterWithoutTaskId_when_handle_then_persistOnlyAndAck() throws IOException {
        Map<String, Object> payload = Map.of("msgId", "msg-002");
        Message message = buildMessage(payload, "jinshu.render.segment", 1);

        consumer.handleDeadLetter(message, channel);

        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO task.mq_dead_letter " +
                   "(msg_id, task_id, message_type, source_queue, error_message, payload, x_death_count, status, created_at) " +
                   "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 'PROCESSED', ?)"),
                eq("msg-002"), eq(null), eq("PDF_SEGMENT"), eq("jinshu.render.segment"),
                any(String.class), any(String.class), eq(1), any(java.time.LocalDateTime.class)
        );
        verify(channel, times(1)).basicAck(1L, false);
    }

    @Test
    @DisplayName("处理异常消息：即使解析失败也应 ACK，避免死循环")
    void given_invalidMessageBody_when_handle_then_ackOnly() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(2L);
        props.setConsumerQueue(MqConstants.DLQ_QUEUE);
        Message message = new Message("not-json".getBytes(), props);

        consumer.handleDeadLetter(message, channel);

        verify(jdbcTemplate, times(0)).update(any(String.class), any(Object[].class));
        verify(channel, times(1)).basicAck(2L, false);
    }

    @Test
    @DisplayName("从 x-death header 推断来源队列与死亡次数")
    void given_xDeathHeader_when_handle_then_extractQueueAndCount() throws IOException {
        Map<String, Object> xDeath = Map.of(
                "queue", "jinshu.export",
                "count", 2L,
                "reason", "rejected"
        );
        Map<String, Object> payload = Map.of("taskId", 99L);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(3L);
        props.setConsumerQueue(MqConstants.DLQ_QUEUE);
        props.setHeader("x-death", List.of(xDeath));
        Message message = new Message(objectMapper.writeValueAsBytes(payload), props);

        consumer.handleDeadLetter(message, channel);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, times(1)).update(eq("INSERT INTO task.mq_dead_letter " +
                "(msg_id, task_id, message_type, source_queue, error_message, payload, x_death_count, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 'PROCESSED', ?)"), captor.capture());
        Object[] args = captor.getValue();
        assertThat(args[2]).isEqualTo("EXPORT");
        assertThat(args[3]).isEqualTo("jinshu.export");
        assertThat(args[6]).isEqualTo(2);
    }

    private Message buildMessage(Map<String, Object> payload, String sourceQueue, int deathCount) throws IOException {
        Map<String, Object> xDeath = Map.of(
                "queue", sourceQueue,
                "count", (long) deathCount,
                "reason", "rejected"
        );
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        props.setConsumerQueue(MqConstants.DLQ_QUEUE);
        props.setHeader("x-death", List.of(xDeath));
        return new Message(objectMapper.writeValueAsBytes(payload), props);
    }
}
