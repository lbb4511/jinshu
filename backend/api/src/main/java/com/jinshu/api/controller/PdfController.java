package com.jinshu.api.controller;

import com.jinshu.api.service.PdfTaskService;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfTaskService pdfTaskService;

    @GetMapping("/estimate")
    public Result<Map<String, Object>> estimate(@RequestParam Long reportId) {
        return Result.success(pdfTaskService.estimate(reportId));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submitPdf(@RequestBody PdfTaskService.PdfSubmitRequest request) {
        Long taskId = pdfTaskService.createPdfTask(request);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> getProgress(@PathVariable Long taskId) {
        return Result.success(pdfTaskService.getTaskProgress(taskId));
    }

    @GetMapping("/{taskId}/download")
    public Result<Map<String, Object>> getDownloadLink(@PathVariable Long taskId) {
        return Result.success(pdfTaskService.generateDownloadLink(taskId));
    }
}
