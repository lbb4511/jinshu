package com.jinshu.batch.writer;

import com.jinshu.batch.dao.ReportDataMapper;
import com.jinshu.common.entity.ReportData;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReportBatchWriter {

    private static final int BATCH_SIZE = 1000;

    private final ReportDataMapper reportDataMapper;

    private final Long tenantId;

    private final Long reportId;

    private final List<ReportData> buffer = new ArrayList<>();

    private int totalWritten = 0;

    public ReportBatchWriter(ReportDataMapper reportDataMapper, Long tenantId, Long reportId) {
        this.reportDataMapper = reportDataMapper;
        this.tenantId = tenantId;
        this.reportId = reportId;
    }

    public synchronized void write(Map<String, Object> cells, int rowNo) {
        ReportData record = new ReportData();
        record.setTenantId(tenantId);
        record.setReportId(reportId);
        record.setRowNo(rowNo);
        record.setDataJson(toJsonString(cells));
        record.setCreatedAt(LocalDateTime.now());
        buffer.add(record);

        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    public synchronized void flush() {
        if (buffer.isEmpty()) return;
        List<ReportData> batch = new ArrayList<>(buffer);
        buffer.clear();
        reportDataMapper.batchInsert(batch);
        totalWritten += batch.size();
        log.debug("Batch written: {} rows (total: {})", batch.size(), totalWritten);
    }

    public int getTotalWritten() {
        return totalWritten;
    }

    private String toJsonString(Map<String, Object> cells) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : cells.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(escapeJson(val.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
