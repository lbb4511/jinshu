package com.jinshu.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNameUtil {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateExportFileName(Long tenantId, Long reportId, String format) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String ext = switch (format.toUpperCase()) {
            case "EXCEL" -> "xlsx";
            case "CSV" -> "csv";
            default -> "dat";
        };
        return String.format("%d_%d_%s.%s", tenantId, reportId, timestamp, ext);
    }

    public static String generateExportFilePath(Long tenantId, Long reportId, String format) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String ext = switch (format.toUpperCase()) {
            case "EXCEL" -> "xlsx";
            case "CSV" -> "csv";
            default -> "dat";
        };
        return String.format("/data/output/%d/%d_%s.%s", tenantId, reportId, timestamp, ext);
    }

    public static String generateTempFilePath(Long tenantId, Long taskId) {
        return String.format("/data/temp/%d/export_%d.tmp", tenantId, taskId);
    }

    public static String getSafeFileName(String original) {
        if (original == null) return "export";
        return original.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
