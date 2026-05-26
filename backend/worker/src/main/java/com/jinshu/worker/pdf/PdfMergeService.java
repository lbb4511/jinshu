package com.jinshu.worker.pdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfMergeService {

    private final PdfTaskRepository taskRepository;

    public void mergeSegments(Long taskId, Map<String, Object> config) {
        List<Map<String, Object>> segments = (List<Map<String, Object>>) config.get("segments");
        if (segments == null || segments.isEmpty()) {
            log.warn("No segments to merge for task {}", taskId);
            taskRepository.updateTaskError(taskId, "无分片可合并");
            return;
        }

        String outputPath = (String) config.get("outputPath");
        if (outputPath == null) {
            outputPath = "/data/output/" + taskId + ".pdf";
            config.put("outputPath", outputPath);
        }

        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.setDestinationFileName(outputPath);

            List<File> segmentFiles = new ArrayList<>();
            for (Map<String, Object> seg : segments) {
                String segPath = (String) seg.get("outputPath");
                if (segPath != null) {
                    File segFile = new File(segPath);
                    if (segFile.exists() && segFile.length() > 0) {
                        segmentFiles.add(segFile);
                        merger.addSource(segFile);
                    } else {
                        log.warn("Segment file missing or empty: {}", segPath);
                    }
                }
            }

            if (segmentFiles.isEmpty()) {
                taskRepository.updateTaskError(taskId, "无有效分片文件可合并");
                return;
            }

            merger.mergeDocuments(null);

            File outputFile = new File(outputPath);
            if (outputFile.exists() && outputFile.length() > 0) {
                log.info("PDF merge completed: taskId={}, output={}, size={}",
                    taskId, outputPath, outputFile.length());

                config.put("fallback", false);
                taskRepository.completeTask(taskId, outputPath);

                cleanupSegmentFiles(segmentFiles);
            } else {
                taskRepository.updateTaskError(taskId, "合并后文件为空");
            }
        } catch (IOException e) {
            log.error("PDF merge failed for task {}", taskId, e);
            handleMergeFailure(taskId, e);
        }
    }

    private void handleMergeFailure(Long taskId, Exception e) {
        String error = "PDF 合并失败: " + e.getMessage();
        taskRepository.updateTaskError(taskId, error);
    }

    private void cleanupSegmentFiles(List<File> segmentFiles) {
        for (File f : segmentFiles) {
            if (f.exists() && !f.delete()) {
                log.warn("Failed to delete segment file: {}", f.getAbsolutePath());
            }
        }
    }
}
