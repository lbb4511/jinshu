package com.jinshu.worker.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PdfTaskRepository {

    private static final String REDIS_KEY_PREFIX = "jinshu:pdf:progress:";
    private static final String REDIS_CONFIG_PREFIX = "jinshu:task:config:";

    private final PdfTaskMapper taskMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getTaskConfig(Long taskId) {
        String cached = (String) redisTemplate.opsForHash().get(REDIS_CONFIG_PREFIX + taskId, "config");
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached config for task {}", taskId, e);
            }
        }

        var task = taskMapper.selectById(taskId);
        if (task == null || task.getParameters() == null) {
            return null;
        }

        try {
            Map<String, Object> config = objectMapper.readValue(task.getParameters(), Map.class);
            redisTemplate.opsForHash().put(REDIS_CONFIG_PREFIX + taskId, "config", task.getParameters());
            redisTemplate.expire(REDIS_CONFIG_PREFIX + taskId, Duration.ofHours(2));
            return config;
        } catch (Exception e) {
            log.error("Failed to parse task config for task {}", taskId, e);
            return null;
        }
    }

    public void updateTaskConfig(Long taskId, String configJson) {
        com.jinshu.common.entity.Task task = new com.jinshu.common.entity.Task();
        task.setId(taskId);
        task.setParameters(configJson);
        taskMapper.update(task);

        redisTemplate.opsForHash().put(REDIS_CONFIG_PREFIX + taskId, "config", configJson);
        redisTemplate.expire(REDIS_CONFIG_PREFIX + taskId, Duration.ofHours(2));

        initProgressIfNeeded(taskId);
    }

    public void updateTaskStatus(Long taskId, String status, LocalDateTime startedAt) {
        com.jinshu.common.entity.Task task = new com.jinshu.common.entity.Task();
        task.setId(taskId);
        task.setStatus(status);
        task.setStartedAt(startedAt);
        taskMapper.update(task);
        updateRedisStatus(taskId, status);
    }

    public void updateTaskProgress(Long taskId, int progress) {
        com.jinshu.common.entity.Task task = new com.jinshu.common.entity.Task();
        task.setId(taskId);
        task.setProgress(progress);
        taskMapper.update(task);

        String key = redisKey(taskId);
        redisTemplate.opsForHash().put(key, "progress", String.valueOf(progress));
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    public void updateTaskError(Long taskId, String errorMessage) {
        com.jinshu.common.entity.Task task = new com.jinshu.common.entity.Task();
        task.setId(taskId);
        task.setStatus("FAILED");
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.update(task);
        updateRedisStatus(taskId, "FAILED");
        log.error("Task {} failed: {}", taskId, errorMessage);
    }

    public void completeTask(Long taskId, String outputPath) {
        com.jinshu.common.entity.Task task = new com.jinshu.common.entity.Task();
        task.setId(taskId);
        task.setStatus("SUCCESS");
        task.setProgress(100);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.update(task);
        updateRedisStatus(taskId, "SUCCESS");
        log.info("Task {} completed successfully, output: {}", taskId, outputPath);
    }

    private void initProgressIfNeeded(Long taskId) {
        String key = redisKey(taskId);
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForHash().put(key, "status", "PENDING");
            redisTemplate.opsForHash().put(key, "progress", "0");
            redisTemplate.expire(key, Duration.ofHours(2));
        }
    }

    private void updateRedisStatus(Long taskId, String status) {
        redisTemplate.opsForHash().put(redisKey(taskId), "status", status);
        redisTemplate.expire(redisKey(taskId), Duration.ofHours(2));
    }

    private String redisKey(Long taskId) {
        return REDIS_KEY_PREFIX + taskId;
    }
}
