package com.jinshu.api.controller;

import com.jinshu.api.service.ReportService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.Report;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "CREATE_REPORT", targetType = "REPORT")
    public Result<Report> createReport(@RequestBody ReportService.CreateReportRequest request) {
        Report report = reportService.createReport(request);
        return Result.success(report);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public Result<PageResult<Report>> listReports(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Report> result = reportService.listReports(name, status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public Result<Report> getReport(@PathVariable Long id) {
        Report report = reportService.getReportById(id);
        return Result.success(report);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "UPDATE_REPORT", targetType = "REPORT")
    public Result<Report> updateReport(@PathVariable Long id, @RequestBody ReportService.UpdateReportRequest request) {
        Report report = reportService.updateReport(id, request);
        return Result.success(report);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "DELETE_REPORT", targetType = "REPORT")
    public Result<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "SUBMIT_REVIEW", targetType = "REPORT")
    public Result<Report> submitForReview(@PathVariable Long id, @RequestBody ReportService.SubmitReviewRequest request) {
        Report report = reportService.submitForReview(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "APPROVE_REPORT", targetType = "REPORT")
    public Result<Report> approveReport(@PathVariable Long id, @RequestBody ReportService.ReviewRequest request) {
        Report report = reportService.approveReport(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "REJECT_REPORT", targetType = "REPORT")
    public Result<Report> rejectReport(@PathVariable Long id, @RequestBody ReportService.ReviewRequest request) {
        Report report = reportService.rejectReport(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @AuditLog(operation = "PUBLISH_REPORT", targetType = "REPORT")
    public Result<Report> publishReport(@PathVariable Long id) {
        Report report = reportService.publishReport(id);
        return Result.success(report);
    }
}
