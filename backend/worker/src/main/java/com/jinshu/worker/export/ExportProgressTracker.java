package com.jinshu.worker.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.dto.ExportProgress;
import com.jinshu.common.dto.ExportProgressEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportProgressTracker {

    private static final String KEY_PREFIX = "jinshu:export:progress:";
    private static final String EVENT_CHANNEL = "jinshu:export:progress:events";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void init(Long taskId) {
        init(taskId, 0L);
    }

    public void init(Long taskId, long totalRows) {
        String key = key(taskId);
        Map<String, String> fields = new HashMap<>();
        fields.put("status", "PENDING");
        fields.put("progress", "0");
        fields.put("processedRows", "0");
        fields.put("totalRows", String.valueOf(totalRows));
        fields.put("message", "");
        fields.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    public void updateStatus(Long taskId, String status) {
        updateStatus(taskId, status, null);
    }

    public void updateStatus(Long taskId, String status, String message) {
        String key = key(taskId);
        redisTemplate.opsForHash().put(key, "status", status);
        redisTemplate.opsForHash().put(key, "updatedAt", String.valueOf(System.currentTimeMillis()));
        if (message != null) {
            redisTemplate.opsForHash().put(key, "message", message);
        }

        jdbcTemplate.update("UPDATE task.task SET status = ? WHERE id = ?", status, taskId);

        if (isTerminalStatus(status)) {
            redisTemplate.opsForHash().put(key, "progress", "100");
            jdbcTemplate.update("UPDATE task.task SET progress = 100, completed_at = NOW() WHERE id = ?", taskId);
        }

        publishEvent(taskId);
    }

    public void updateProgress(Long taskId, int processedRows) {
        String key = key(taskId);
        Long totalRows = getTotalRows(taskId);
        int progress = calculateProgress(processedRows, totalRows);

        redisTemplate.opsForHash().put(key, "processedRows", String.valueOf(processedRows));
        redisTemplate.opsForHash().put(key, "progress", String.valueOf(progress));
        redisTemplate.opsForHash().put(key, "updatedAt", String.valueOf(System.currentTimeMillis()));

        jdbcTemplate.update("UPDATE task.task SET progress = ? WHERE id = ?", progress, taskId);

        publishEvent(taskId);
    }

    public void setTotalRows(Long taskId, long totalRows) {
        String key = key(taskId);
        redisTemplate.opsForHash().put(key, "totalRows", String.valueOf(totalRows));
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    public Long getTotalRows(Long taskId) {
        String value = (String) redisTemplate.opsForHash().get(key(taskId), "totalRows");
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid totalRows value in Redis for task {}: {}", taskId, value);
            return null;
        }
    }

    public ExportProgress getProgress(Long taskId) {
        String key = key(taskId);
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

    public String getTaskConfig(Long taskId) {
        String config = (String) redisTemplate.opsForHash().get("jinshu:task:config:" + taskId, "config");
        if (config == null) {
            config = jdbcTemplate.queryForObject(
                    "SELECT parameters FROM task.task WHERE id = ?",
                    String.class,
                    taskId);
        }
        return config;
    }

    public void saveResultFileInfo(Long taskId, String filePath, long fileSize, String fileName) {
        String key = key(taskId);
        redisTemplate.opsForHash().put(key, "resultFilePath", filePath);
        redisTemplate.opsForHash().put(key, "resultFileSize", String.valueOf(fileSize));
        redisTemplate.opsForHash().put(key, "resultFileName", fileName);
        redisTemplate.opsForHash().put(key, "updatedAt", String.valueOf(System.currentTimeMillis()));

        jdbcTemplate.update(
                "UPDATE task.task SET result = ?::jsonb, result_file_path = ?, result_file_size = ?, result_file_name = ? WHERE id = ?",
                buildResultJson(filePath, fileSize, fileName),
                filePath, fileSize, fileName,
                taskId);
    }

    private String buildResultJson(String filePath, long fileSize, String fileName) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "filePath", filePath,
                    "fileSize", fileSize,
                    "fileName", fileName));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize result file info", e);
        }
    }

    private void publishEvent(Long taskId) {
        try {
            ExportProgress progress = getProgress(taskId);
            if (progress == null) {
                return;
            }
            ExportProgressEvent event = ExportProgressEvent.builder()
                    .taskId(taskId)
                    .status(progress.getStatus())
                    .progress(progress.getProgress())
                    .processedRows(progress.getProcessedRows())
                    .totalRows(progress.getTotalRows())
                    .message(progress.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
            redisTemplate.convertAndSend(EVENT_CHANNEL, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize export progress event for task {}: {}", taskId, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to publish export progress event for task {}: {}", taskId, e.getMessage());
        }
    }

    private int calculateProgress(int processedRows, Long totalRows) {
        if (totalRows == null || totalRows <= 0) {
            return 0;
        }
        if (processedRows >= totalRows) {
            return 100;
        }
        return (int) Math.round(processedRows * 100.0 / totalRows);
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private String key(Long taskId) {
        return KEY_PREFIX + taskId;
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
