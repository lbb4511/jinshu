package com.jinshu.worker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExcelExportHandler excelExportHandler;

    private final CsvExportHandler csvExportHandler;

    private final ExportProgressTracker progressTracker;

    private final ObjectMapper objectMapper;

    public void executeExport(Long taskId) {
        progressTracker.init(taskId);
        progressTracker.updateStatus(taskId, "PROCESSING");

        String configJson = progressTracker.getTaskConfig(taskId);
        if (configJson == null) {
            throw new IllegalArgumentException("Task config not found: " + taskId);
        }

        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse export config", e);
        }

        String format = (String) config.getOrDefault("format", "EXCEL");
        String outputPath = (String) config.get("outputPath");
        Long reportId = Long.valueOf(config.get("reportId").toString());

        try {
            switch (format.toUpperCase()) {
                case "EXCEL" -> excelExportHandler.export(taskId, reportId, outputPath);
                case "CSV" -> csvExportHandler.export(taskId, reportId, outputPath);
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            }
            progressTracker.updateStatus(taskId, "SUCCESS");
        } catch (Exception e) {
            progressTracker.updateStatus(taskId, "FAILED");
            throw e;
        }
    }

    public void markFailed(Long taskId, String errorMessage) {
        progressTracker.updateStatus(taskId, "FAILED");
        log.error("Export task {} failed: {}", taskId, errorMessage);
    }
}
