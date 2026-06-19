package com.jinshu.worker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExcelExportHandler excelExportHandler;

    private final CsvExportHandler csvExportHandler;

    private final ExportProgressTracker progressTracker;

    private final ObjectMapper objectMapper;

    private final JdbcTemplate jdbcTemplate;

    private final BusinessMetrics businessMetrics;

    public void executeExport(Long taskId) {
        long startNs = System.nanoTime();
        try {
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
            Long tenantId = Long.valueOf(config.get("tenantId").toString());
            String templateFilePath = resolveTemplateFilePath(tenantId, config.get("templateId"));

            try {
                switch (format.toUpperCase()) {
                    case "EXCEL" -> excelExportHandler.export(taskId, reportId, outputPath, templateFilePath);
                    case "CSV" -> csvExportHandler.export(taskId, reportId, outputPath);
                    default -> throw new IllegalArgumentException("Unsupported format: " + format);
                }
                saveResultFileInfo(taskId, format, outputPath);
                progressTracker.updateStatus(taskId, "SUCCESS");
            } catch (RuntimeException e) {
                progressTracker.updateStatus(taskId, "FAILED", e.getMessage());
                throw e;
            }
        } finally {
            businessMetrics.recordExportDuration((System.nanoTime() - startNs) / 1_000_000_000.0);
        }
    }

    public void markFailed(Long taskId, String errorMessage) {
        progressTracker.updateStatus(taskId, "FAILED");
        log.error("Export task {} failed: {}", taskId, errorMessage);
    }

    private String resolveTemplateFilePath(Long tenantId, Object templateIdObj) {
        if (templateIdObj == null) {
            return null;
        }
        Long templateId = Long.valueOf(templateIdObj.toString());
        try {
            String filePath = jdbcTemplate.queryForObject(
                    "SELECT file_path FROM meta.excel_template_file WHERE id = ? AND tenant_id = ?",
                    String.class, templateId, tenantId);
            if (filePath == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "模板文件不存在: " + templateId);
            }
            return filePath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve template file path: " + templateId, e);
        }
    }

    private void saveResultFileInfo(Long taskId, String format, String outputPath) {
        try {
            String actualPath = switch (format.toUpperCase()) {
                case "CSV" -> outputPath.replace(".csv", ".zip");
                default -> outputPath;
            };
            Path path = Path.of(actualPath);
            if (!Files.exists(path)) {
                throw new RuntimeException("Output file not found: " + actualPath);
            }
            long fileSize = Files.size(path);
            String fileName = path.getFileName().toString();
            progressTracker.saveResultFileInfo(taskId, actualPath, fileSize, fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save result file info", e);
        }
    }
}
