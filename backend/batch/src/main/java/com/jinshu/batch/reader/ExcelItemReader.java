package com.jinshu.batch.reader;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.jinshu.batch.model.ExcelImportRow;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ExcelItemReader {

    private static final int BATCH_SIZE = 1000;

    private final String filePath;

    private final List<String> columnHeaders;

    private final int shardStartRow;

    private final int shardEndRow;

    private volatile boolean stopped = false;

    public ExcelItemReader(String filePath, List<String> columnHeaders,
                           int shardStartRow, int shardEndRow) {
        this.filePath = filePath;
        this.columnHeaders = columnHeaders;
        this.shardStartRow = shardStartRow;
        this.shardEndRow = shardEndRow;
    }

    public void read(Consumer<List<ExcelImportRow>> batchConsumer, Consumer<Integer> progressCallback) {
        BatchCollector collector = new BatchCollector(
                columnHeaders, shardStartRow, shardEndRow,
                batchConsumer, progressCallback, BATCH_SIZE);

        EasyExcel.read(filePath, collector)
                .headRowNumber(1)
                .sheet()
                .doRead();

        log.info("Excel read complete for file: {}", filePath);
    }

    public void stop() {
        this.stopped = true;
    }

    private class BatchCollector extends AnalysisEventListener<Map<Integer, Object>> {

        private final List<String> columnHeaders;

        private final int shardStartRow;

        private final int shardEndRow;

        private final Consumer<List<ExcelImportRow>> batchConsumer;

        private final Consumer<Integer> progressCallback;

        private final int batchSize;

        private final List<ExcelImportRow> buffer = new ArrayList<>();

        private int totalRead = 0;

        BatchCollector(List<String> columnHeaders, int shardStartRow, int shardEndRow,
                       Consumer<List<ExcelImportRow>> batchConsumer,
                       Consumer<Integer> progressCallback, int batchSize) {
            this.columnHeaders = columnHeaders;
            this.shardStartRow = shardStartRow;
            this.shardEndRow = shardEndRow;
            this.batchConsumer = batchConsumer;
            this.progressCallback = progressCallback;
            this.batchSize = batchSize;
        }

        @Override
        public void invoke(Map<Integer, Object> rowData, AnalysisContext context) {
            if (stopped) {
                context.interrupt();
                return;
            }

            int currentRowNo = context.readRowHolder().getRowIndex() + 1;

            if (currentRowNo <= shardStartRow || currentRowNo > shardEndRow) {
                return;
            }

            totalRead++;

            if (isRowEmpty(rowData)) {
                return;
            }

            ExcelImportRow row = new ExcelImportRow();
            row.setRowNo(currentRowNo);

            for (int i = 0; i < columnHeaders.size(); i++) {
                String colName = columnHeaders.get(i);
                Object value = rowData.get(i);
                row.addCell(colName, value);
            }

            buffer.add(row);

            if (buffer.size() >= batchSize) {
                flush();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!buffer.isEmpty()) {
                flush();
            }
            log.info("Shard read complete. Rows read: {}", totalRead);
        }

        private void flush() {
            List<ExcelImportRow> batch = new ArrayList<>(buffer);
            buffer.clear();
            batchConsumer.accept(batch);
            progressCallback.accept(totalRead);
        }

        private boolean isRowEmpty(Map<Integer, Object> rowData) {
            if (rowData == null || rowData.isEmpty()) {
                return true;
            }
            return rowData.values().stream()
                    .allMatch(v -> v == null || (v instanceof String && ((String) v).trim().isEmpty()));
        }
    }
}
