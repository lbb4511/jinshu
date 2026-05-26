package com.jinshu.worker.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsvExportHandler {

    private static final int FLUSH_INTERVAL = 1000;

    private final ExportProgressTracker progressTracker;

    public void export(Long taskId, Long reportId, String outputPath) {
        String csvPath = outputPath.replace(".csv", ".csv");
        String zipPath = outputPath.replace(".csv", ".zip");

        log.info("Starting CSV export: taskId={}, csvPath={}", taskId, csvPath);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvPath), StandardCharsets.UTF_8))) {

            writer.write('\uFEFF');
            writer.write("col_1,col_2,col_3");
            writer.newLine();

            int count = 0;
            List<List<Object>> rows = fetchData(taskId);
            for (List<Object> row : rows) {
                writer.write(escapeCsv(row));
                writer.newLine();
                count++;

                if (count % FLUSH_INTERVAL == 0) {
                    writer.flush();
                    progressTracker.updateProgress(taskId, count);
                }
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("CSV write failed", e);
        }

        compressToZip(csvPath, zipPath);
        log.info("CSV export completed: taskId={}, zipPath={}", taskId, zipPath);
    }

    private List<List<Object>> fetchData(Long taskId) {
        return List.of(List.of("d1", "d2", "d3"));
    }

    private String escapeCsv(List<Object> row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) sb.append(',');
            String val = row.get(i) != null ? row.get(i).toString() : "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                val = val.startsWith("=") || val.startsWith("+") || val.startsWith("-") || val.startsWith("@")
                        ? "'" + val : val;
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    private void compressToZip(String csvPath, String zipPath) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath));
             FileInputStream fis = new FileInputStream(csvPath)) {

            zos.putNextEntry(new ZipEntry(new File(csvPath).getName()));
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("ZIP compression failed", e);
        }

        new File(csvPath).delete();
    }
}
