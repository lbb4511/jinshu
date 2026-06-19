package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.dto.ExportProgress;
import com.jinshu.common.dto.ExportProgressEvent;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 导出任务 SSE 进度订阅服务。
 *
 * 维护每个任务对应的 SseEmitter，通过 Redis Pub/Sub 接收 Worker 进度事件并实时推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportProgressService {

    private static final long SSE_TIMEOUT = 600_000L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30L;
    private static final String PROGRESS_KEY_PREFIX = "jinshu:export:progress:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskMapper taskMapper;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "export-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /**
     * 订阅指定导出任务的实时进度 SSE 流。
     *
     * @param taskId 任务 ID
     * @return SSE 发射器
     */
    public SseEmitter subscribe(Long taskId) {
        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        SseEmitter previous = emitters.put(taskId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> emitters.remove(taskId, emitter));
        emitter.onTimeout(() -> emitters.remove(taskId, emitter));
        emitter.onError(e -> emitters.remove(taskId, emitter));

        sendCurrentProgress(taskId, emitter);

        if (isTerminalStatus(task.getStatus())) {
            completeEmitter(taskId, emitter);
            return emitter;
        }

        scheduleHeartbeat(taskId, emitter);
        return emitter;
    }

    /**
     * 处理来自 Redis Pub/Sub 的进度事件。
     *
     * @param event 进度事件
     */
    public void handleProgressEvent(ExportProgressEvent event) {
        if (event == null || event.getTaskId() == null) {
            return;
        }
        SseEmitter emitter = emitters.get(event.getTaskId());
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(event.getTimestamp()))
                    .name("progress")
                    .data(event));
            if (isTerminalStatus(event.getStatus())) {
                completeEmitter(event.getTaskId(), emitter);
            }
        } catch (IOException e) {
            log.warn("Failed to send SSE progress event for task {}: {}", event.getTaskId(), e.getMessage());
            completeEmitter(event.getTaskId(), emitter);
        }
    }

    /**
     * 处理来自 Redis Pub/Sub 的原始消息字符串。
     *
     * @param message 原始 JSON 消息
     */
    public void handleProgressMessage(String message) {
        try {
            ExportProgressEvent event = objectMapper.readValue(message, ExportProgressEvent.class);
            handleProgressEvent(event);
        } catch (Exception e) {
            log.warn("Failed to parse export progress message: {}", message, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        for (ScheduledFuture<?> future : heartbeatTasks.values()) {
            future.cancel(false);
        }
        heartbeatTasks.clear();
        heartbeatExecutor.shutdownNow();
        for (SseEmitter emitter : emitters.values()) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // ignored
            }
        }
        emitters.clear();
    }

    private void sendCurrentProgress(Long taskId, SseEmitter emitter) {
        try {
            ExportProgress progress = getProgressFromRedis(taskId);
            if (progress == null) {
                progress = getProgressFromTask(taskId);
            }
            if (progress != null) {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));
            }
        } catch (IOException e) {
            log.warn("Failed to send initial SSE progress for task {}: {}", taskId, e.getMessage());
            completeEmitter(taskId, emitter);
        }
    }

    private ExportProgress getProgressFromRedis(Long taskId) {
        String key = PROGRESS_KEY_PREFIX + taskId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return ExportProgress.builder()
                .taskId(taskId)
                .status(getString(entries, "status"))
                .progress(getInt(entries, "progress"))
                .processedRows(getLong(entries, "processedRows"))
                .totalRows(getLong(entries, "totalRows"))
                .message(getString(entries, "message"))
                .updatedAt(getLong(entries, "updatedAt"))
                .build();
    }

    private ExportProgress getProgressFromTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        return ExportProgress.builder()
                .taskId(taskId)
                .status(task.getStatus())
                .progress(task.getProgress())
                .updatedAt(task.getCreatedAt() != null
                        ? task.getCreatedAt().toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli()
                        : null)
                .build();
    }

    private void scheduleHeartbeat(Long taskId, SseEmitter emitter) {
        ScheduledFuture<?> future = heartbeatExecutor.scheduleAtFixedRate(() -> {
            SseEmitter current = emitters.get(taskId);
            if (current != emitter) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                completeEmitter(taskId, emitter);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        heartbeatTasks.put(taskId, future);
    }

    private void completeEmitter(Long taskId, SseEmitter emitter) {
        ScheduledFuture<?> future = heartbeatTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignored
        }
        emitters.remove(taskId, emitter);
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private String getString(Map<Object, Object> entries, String field) {
        Object value = entries.get(field);
        return value != null ? value.toString() : null;
    }

    private Integer getInt(Map<Object, Object> entries, String field) {
        String value = getString(entries, field);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLong(Map<Object, Object> entries, String field) {
        String value = getString(entries, field);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
