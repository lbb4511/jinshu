package com.jinshu.worker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportProgressTracker - 导出进度追踪器")
class ExportProgressTrackerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private ExportProgressTracker tracker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tracker = new ExportProgressTracker(redisTemplate, jdbcTemplate, objectMapper);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("init: 初始化任务进度到 Redis")
    void given_taskId_when_init_then_initRedisHash() {
        tracker.init(1L, 100L);

        verify(hashOperations, times(1)).putAll(eq("jinshu:export:progress:1"), any(Map.class));
        verify(redisTemplate, times(1)).expire(eq("jinshu:export:progress:1"), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("updateStatus: 更新状态并同步数据库")
    void given_taskId_when_updateStatus_then_updateDbAndRedis() {
        tracker.updateStatus(1L, "PROCESSING");

        verify(hashOperations, times(1)).put("jinshu:export:progress:1", "status", "PROCESSING");
        verify(jdbcTemplate, times(1)).update("UPDATE task.task SET status = ? WHERE id = ?", "PROCESSING", 1L);
    }

    @Test
    @DisplayName("updateStatus SUCCESS: 同步数据库并将进度设为 100")
    void given_taskId_when_updateStatusSuccess_then_setProgress100() {
        tracker.updateStatus(1L, "SUCCESS");

        verify(hashOperations, times(1)).put("jinshu:export:progress:1", "status", "SUCCESS");
        verify(hashOperations, times(1)).put("jinshu:export:progress:1", "progress", "100");
        verify(jdbcTemplate, times(1)).update(
                "UPDATE task.task SET progress = 100, completed_at = NOW() WHERE id = ?", 1L);
    }

    @Test
    @DisplayName("updateProgress: 根据总行数计算百分比")
    void given_processedRows_when_updateProgress_then_calculatePercentage() {
        when(hashOperations.get("jinshu:export:progress:1", "totalRows")).thenReturn("100");

        tracker.updateProgress(1L, 50);

        verify(hashOperations, times(1)).put("jinshu:export:progress:1", "processedRows", "50");
        verify(hashOperations, times(1)).put("jinshu:export:progress:1", "progress", "50");
        verify(jdbcTemplate, times(1)).update("UPDATE task.task SET progress = ? WHERE id = ?", 50, 1L);
    }

    @Test
    @DisplayName("getTotalRows: 从 Redis 读取总行数")
    void given_totalRowsInRedis_when_getTotalRows_then_returnValue() {
        when(hashOperations.get("jinshu:export:progress:1", "totalRows")).thenReturn("200");

        Long totalRows = tracker.getTotalRows(1L);

        assertThat(totalRows).isEqualTo(200L);
    }

    @Test
    @DisplayName("getProgress: 从 Redis 读取完整进度")
    void given_progressInRedis_when_getProgress_then_returnProgress() {
        when(hashOperations.entries("jinshu:export:progress:1")).thenReturn(Map.of(
                "status", "PROCESSING",
                "progress", "30",
                "processedRows", "30",
                "totalRows", "100",
                "updatedAt", "1234567890"
        ));

        var progress = tracker.getProgress(1L);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo("PROCESSING");
        assertThat(progress.getProgress()).isEqualTo(30);
        assertThat(progress.getTotalRows()).isEqualTo(100L);
    }
}
