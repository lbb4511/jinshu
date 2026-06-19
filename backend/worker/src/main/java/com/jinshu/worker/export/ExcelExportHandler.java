package com.jinshu.worker.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.worker.dao.ReportDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelExportHandler {

    private static final int BATCH_SIZE = 1000;

    private final ExportProgressTracker progressTracker;
    private final ReportDataMapper reportDataMapper;
    private final ObjectMapper objectMapper;

    public void export(Long taskId, Long reportId, String outputPath, String templateFilePath) {
        log.info("Starting Excel export: taskId={}, reportId={}, outputPath={}, templateFilePath={}",
                taskId, reportId, outputPath, templateFilePath);

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "租户上下文缺失，无法导出");
        }

        Path outputDir = Path.of(outputPath).getParent();
        if (outputDir != null) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output directory: " + outputPath, e);
            }
        }

        boolean useTemplate = templateFilePath != null && !templateFilePath.isBlank();
        if (useTemplate) {
            Path templatePath = Path.of(templateFilePath);
            if (!Files.exists(templatePath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "模板文件不存在: " + templateFilePath);
            }
            if (!Files.isRegularFile(templatePath)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件路径非法: " + templateFilePath);
            }
        }

        List<List<Object>> buffer = new ArrayList<>(BATCH_SIZE);
        try (ExcelWriter writer = createWriter(outputPath, templateFilePath, useTemplate)) {
            WriteSheet writeSheet = createWriteSheet(useTemplate);

            if (!useTemplate) {
                buffer.add(getHeaderRow());
                writer.write(buffer, writeSheet);
                buffer.clear();
            }

            ResultHandler<Map<String, Object>> handler = context -> {
                Map<String, Object> row = context.getResultObject();
                buffer.add(convertRow(row));
                if (buffer.size() >= BATCH_SIZE) {
                    writer.write(buffer, writeSheet);
                    buffer.clear();
                }
            };

            progressTracker.setTotalRows(taskId, estimateTotalRows(tenantId, reportId));
            reportDataMapper.selectByReportId(tenantId, reportId, handler);

            if (!buffer.isEmpty()) {
                writer.write(buffer, writeSheet);
                buffer.clear();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed", e);
        }

        if (useTemplate) {
            moveTempToOutput(outputPath);
        }

        Long totalRows = progressTracker.getTotalRows(taskId);
        progressTracker.updateProgress(taskId, totalRows != null ? totalRows.intValue() : 0);
        log.info("Excel export completed: taskId={}", taskId);
    }

    private ExcelWriter createWriter(String outputPath, String templateFilePath, boolean useTemplate) {
        if (useTemplate) {
            String tempOutputPath = outputPath + ".tmp";
            return EasyExcel.write(outputPath)
                    .needHead(false)
                    .withTemplate(templateFilePath)
                    .file(tempOutputPath)
                    .build();
        }
        return EasyExcel.write(outputPath)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .build();
    }

    private WriteSheet createWriteSheet(boolean useTemplate) {
        if (useTemplate) {
            return EasyExcel.writerSheet().build();
        }
        return EasyExcel.writerSheet("Sheet1").build();
    }

    private void moveTempToOutput(String outputPath) {
        Path tempPath = Path.of(outputPath + ".tmp");
        Path targetPath = Path.of(outputPath);
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move temp output file to target: " + outputPath, e);
        }
    }

    private List<Object> getHeaderRow() {
        return List.of("列1", "列2", "列3");
    }

    private List<Object> convertRow(Map<String, Object> row) {
        List<Object> values = new ArrayList<>();
        Object rowNo = row.get("row_no");
        values.add(rowNo);

        Object dataJsonText = row.get("data_json_text");
        if (dataJsonText != null) {
            try {
                Map<String, Object> dataMap = objectMapper.readValue(dataJsonText.toString(), new TypeReference<>() {});
                TreeMap<String, Object> sorted = new TreeMap<>(dataMap);
                values.addAll(sorted.values());
            } catch (Exception e) {
                log.warn("Failed to parse data_json for row_no={}: {}", rowNo, e.getMessage());
                values.add(dataJsonText);
            }
        }
        return values;
    }

    private long estimateTotalRows(Long tenantId, Long reportId) {
        // 简化实现：先统计总行数用于进度计算；后续可改为根据数据量动态估算
        try {
            return reportDataMapper.countByReportId(tenantId, reportId);
        } catch (Exception e) {
            log.warn("Failed to estimate total rows for report {}: {}", reportId, e.getMessage());
            return 0;
        }
    }
}
