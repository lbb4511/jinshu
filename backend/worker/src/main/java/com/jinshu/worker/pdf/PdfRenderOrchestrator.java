package com.jinshu.worker.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfRenderOrchestrator {

    private static final int SEGMENT_SIZE = 50;
    private static final int MAX_PAGES = 500;
    private static final int MAX_SEGMENT_CONCURRENCY = 3;
    private static final int MAX_RETRIES = 3;

    private final PdfTaskRepository taskRepository;
    private final PdfMergeService mergeService;
    private final PdfFallbackService fallbackService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jinshu.render.segment.queue:jinshu.render.segment}")
    private String segmentQueue;

    @Value("${jinshu.domain.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void execute(Long taskId) {
        Map<String, Object> config = taskRepository.getTaskConfig(taskId);
        if (config == null) {
            throw new IllegalArgumentException("Task config not found: " + taskId);
        }

        taskRepository.updateTaskStatus(taskId, "PROCESSING", LocalDateTime.now());

        int pageCount;
        try {
            pageCount = queryPageCount(config);
        } catch (Exception e) {
            log.warn("Failed to query page count, falling back to Thymeleaf for task {}", taskId, e);
            fallbackService.execute(taskId, config);
            return;
        }

        if (pageCount > MAX_PAGES) {
            taskRepository.updateTaskError(taskId, "报表页数超出限制: " + pageCount);
            return;
        }

        config.put("pageCount", pageCount);

        if (pageCount <= SEGMENT_SIZE) {
            renderSingle(taskId, config, pageCount);
        } else {
            renderSegmented(taskId, config, pageCount);
        }
    }

    private void renderSingle(Long taskId, Map<String, Object> config, int pageCount) {
        String renderUrl = buildRenderUrl(config);

        Map<String, Object> segmentConfig = buildSegmentConfig(config, renderUrl, taskId, 1, pageCount, 1);

        rabbitTemplate.convertAndSend(segmentQueue, segmentConfig);
        log.info("Dispatched single PDF render segment: taskId={}, pages={}", taskId, pageCount);

        List<Map<String, Object>> segments = new ArrayList<>();
        Map<String, Object> segment = new HashMap<>();
        segment.put("seq", 1);
        segment.put("pageFrom", 1);
        segment.put("pageTo", pageCount);
        segment.put("status", "PROCESSING");
        segments.add(segment);
        config.put("segments", segments);

        try {
            taskRepository.updateTaskConfig(taskId, objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            log.error("Failed to update task config: taskId={}", taskId, e);
        }
    }

    private void renderSegmented(Long taskId, Map<String, Object> config, int pageCount) {
        int segmentCount = (pageCount + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
        String renderUrl = buildRenderUrl(config);
        List<Map<String, Object>> segments = new ArrayList<>();

        for (int i = 0; i < segmentCount; i++) {
            int pageFrom = i * SEGMENT_SIZE + 1;
            int pageTo = Math.min((i + 1) * SEGMENT_SIZE, pageCount);

            Map<String, Object> segmentConfig = buildSegmentConfig(config, renderUrl, taskId, pageFrom, pageTo, i + 1);
            segmentConfig.put("totalSegments", segmentCount);

            rabbitTemplate.convertAndSend(segmentQueue, segmentConfig);

            Map<String, Object> segment = new HashMap<>();
            segment.put("seq", i + 1);
            segment.put("pageFrom", pageFrom);
            segment.put("pageTo", pageTo);
            segment.put("status", "PROCESSING");
            segments.add(segment);

            if ((i + 1) % MAX_SEGMENT_CONCURRENCY == 0) {
                log.info("Dispatched {} PDF render segments for taskId={}, total={}", i + 1, taskId, segmentCount);
            }
        }

        config.put("segments", segments);
        try {
            taskRepository.updateTaskConfig(taskId, objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            log.error("Failed to update task config: taskId={}", taskId, e);
        }

        log.info("Dispatched all {} PDF render segments for taskId={}", segmentCount, taskId);
    }

    public void onSegmentComplete(Long taskId, int seq, String segmentOutputPath, boolean success, String errorMessage) {
        try {
            Map<String, Object> config = taskRepository.getTaskConfig(taskId);
            if (config == null) return;

            List<Map<String, Object>> segments = (List<Map<String, Object>>) config.get("segments");
            if (segments == null) return;

            for (Map<String, Object> seg : segments) {
                if ((int) seg.get("seq") == seq) {
                    seg.put("status", success ? "SUCCESS" : "FAILED");
                    if (segmentOutputPath != null) {
                        seg.put("outputPath", segmentOutputPath);
                    }
                    if (errorMessage != null) {
                        seg.put("errorMessage", errorMessage);
                    }
                    break;
                }
            }

            int total = segments.size();
            long completed = segments.stream().filter(s -> "SUCCESS".equals(s.get("status"))).count();
            long failed = segments.stream().filter(s -> "FAILED".equals(s.get("status"))).count();
            int progress = (int) (completed * 100 / total);

            config.put("progress", progress);
            taskRepository.updateTaskProgress(taskId, progress);

            try {
                taskRepository.updateTaskConfig(taskId, objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                log.error("Failed to update config for task {}", taskId, e);
            }

            if (completed + failed == total) {
                if (failed > 0) {
                    taskRepository.updateTaskError(taskId, failed + "/" + total + " segments failed");
                } else {
                    mergeService.mergeSegments(taskId, config);
                }
            }
        } catch (Exception e) {
            log.error("Error processing segment completion for task {}", taskId, e);
        }
    }

    public void markFailed(Long taskId, String errorMessage) {
        taskRepository.updateTaskError(taskId, errorMessage);
    }

    private int queryPageCount(Map<String, Object> config) {
        return 0;
    }

    private Map<String, Object> buildSegmentConfig(Map<String, Object> config, String renderUrl,
                                                   Long taskId, int pageFrom, int pageTo, int seq) {
        Map<String, Object> segmentConfig = new HashMap<>(config);
        segmentConfig.put("pageFrom", pageFrom);
        segmentConfig.put("pageTo", pageTo);
        segmentConfig.put("renderUrl", renderUrl);
        segmentConfig.put("parentTaskId", taskId);
        segmentConfig.put("seq", seq);
        segmentConfig.put("cmykEnabled", "CMYK".equals(config.get("colorSpace")));
        segmentConfig.put("watermarkEnabled", Boolean.TRUE.equals(config.get("watermarkEnabled")));
        segmentConfig.put("pdfaEnabled", Boolean.TRUE.equals(config.get("pdfaEnabled")));
        segmentConfig.put("userId", config.get("userId"));
        return segmentConfig;
    }

    private String buildRenderUrl(Map<String, Object> config) {
        Long reportId = Long.valueOf(config.get("reportId").toString());
        return frontendUrl + "/preview?reportId=" + reportId + "&printMode=true";
    }
}
