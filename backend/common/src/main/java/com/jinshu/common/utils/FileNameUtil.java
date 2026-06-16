package com.jinshu.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNameUtil {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String BASE_PATH = System.getProperty("jinshu.file.base-path",
            System.getenv().getOrDefault("JINSHU_FILE_BASE_PATH", "/data/jinshu"));

    private static String outputDir() {
        return BASE_PATH + "/output";
    }

    private static String tempDir() {
        return BASE_PATH + "/temp";
    }

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
        return String.format("%s/%d/%d_%s.%s", outputDir(), tenantId, reportId, timestamp, ext);
    }

    public static String generateTempFilePath(Long tenantId, Long taskId) {
        return String.format("%s/%d/export_%d.tmp", tempDir(), tenantId, taskId);
    }

    public static String getSafeFileName(String original) {
        if (original == null) return "export";
        return original.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
