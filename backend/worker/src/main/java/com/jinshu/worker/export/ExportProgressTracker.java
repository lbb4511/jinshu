package com.jinshu.worker.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportProgressTracker {

    private static final String KEY_PREFIX = "jinshu:export:progress:";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public void init(Long taskId) {
        String key = key(taskId);
        redisTemplate.opsForHash().put(key, "status", "PENDING");
        redisTemplate.opsForHash().put(key, "progress", "0");
        redisTemplate.opsForHash().put(key, "processedRows", "0");
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    public void updateStatus(Long taskId, String status) {
        redisTemplate.opsForHash().put(key(taskId), "status", status);
        jdbcTemplate.update("UPDATE task.task SET status = ? WHERE id = ?", status, taskId);
    }

    public void updateProgress(Long taskId, int processedRows) {
        String key = key(taskId);
        redisTemplate.opsForHash().put(key, "processedRows", String.valueOf(processedRows));
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

    private String key(Long taskId) {
        return KEY_PREFIX + taskId;
    }
}
