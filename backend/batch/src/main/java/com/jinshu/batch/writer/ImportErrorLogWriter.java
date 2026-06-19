package com.jinshu.batch.writer;

import com.jinshu.common.dao.ImportErrorLogMapper;
import com.jinshu.batch.model.ValidationError;
import com.jinshu.common.entity.ImportErrorLog;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ImportErrorLogWriter {

    private static final int BATCH_SIZE = 500;

    private final ImportErrorLogMapper errorLogMapper;

    private final Long tenantId;

    private final Long taskId;

    private final Long reportId;

    private final List<ImportErrorLog> buffer = new ArrayList<>();

    private int totalErrors = 0;

    public ImportErrorLogWriter(ImportErrorLogMapper errorLogMapper,
                                Long tenantId, Long taskId, Long reportId) {
        this.errorLogMapper = errorLogMapper;
        this.tenantId = tenantId;
        this.taskId = taskId;
        this.reportId = reportId;
    }

    public synchronized void writeErrors(List<ValidationError> errors) {
        for (ValidationError ve : errors) {
            ImportErrorLog record = new ImportErrorLog();
            record.setTenantId(tenantId);
            record.setTaskId(taskId);
            record.setReportId(reportId);
            record.setRowNo(ve.getRowNo());
            record.setColumnName(ve.getColumnName());
            record.setErrorType(ve.getErrorType());
            record.setErrorMessage(ve.getErrorMessage());
            record.setCellValue(ve.getCellValue() != null ? truncate(ve.getCellValue().toString(), 200) : null);
            record.setCreatedAt(LocalDateTime.now());
            buffer.add(record);
        }

        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    public synchronized void flush() {
        if (buffer.isEmpty()) return;
        List<ImportErrorLog> batch = new ArrayList<>(buffer);
        buffer.clear();
        errorLogMapper.batchInsert(batch);
        totalErrors += batch.size();
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
