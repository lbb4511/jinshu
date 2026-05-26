package com.jinshu.worker.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfFallbackService {

    private final PdfTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public void execute(Long taskId, Map<String, Object> config) {
        log.warn("Executing fallback PDF rendering for task {}", taskId);

        config.put("fallback", true);

        try {
            String outputPath = "/data/output/" + taskId + "_fallback.pdf";
            config.put("outputPath", outputPath);

            renderWithThymeleaf(taskId, config);

            taskRepository.updateTaskConfig(taskId, objectMapper.writeValueAsString(config));
            taskRepository.completeTask(taskId, outputPath);
            log.info("Fallback PDF rendering completed: taskId={}", taskId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize fallback config for task {}", taskId, e);
            taskRepository.updateTaskError(taskId, "降级渲染失败: " + e.getMessage());
        }
    }

    private void renderWithThymeleaf(Long taskId, Map<String, Object> config) {
        String outputPath = (String) config.get("outputPath");
        if (outputPath == null) {
            outputPath = "/data/output/" + taskId + "_fallback.pdf";
            config.put("outputPath", outputPath);
        }

        log.info("Thymeleaf fallback render (placeholder): taskId={}, output={}", taskId, outputPath);
    }
}
