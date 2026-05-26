package com.jinshu.worker.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelExportHandler {

    private final ExportProgressTracker progressTracker;

    public void export(Long taskId, Long reportId, String outputPath) {
        log.info("Starting Excel export: taskId={}, outputPath={}", taskId, outputPath);

        EasyExcel.write(outputPath)
                .head(getHeader())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("Sheet1")
                .doWrite(() -> streamData(taskId));

        log.info("Excel export completed: taskId={}", taskId);
    }

    private List<List<Object>> streamData(Long taskId) {
        return List.of(List.of("row_1", "data_1"));
    }

    private List<List<String>> getHeader() {
        return List.of(List.of("列1", "列2", "列3"));
    }
}
