package com.jinshu.api.controller;

import com.jinshu.api.service.ReportService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.Report;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireOwner;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "CREATE_REPORT", targetType = "REPORT")
    public Result<Report> createReport(@RequestBody ReportService.CreateReportRequest request) {
        Report report = reportService.createReport(request);
        return Result.success(report);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "AUDITOR", "USER", "VIEWER"})
    public Result<PageResult<Report>> listReports(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Report> result = reportService.listReports(name, status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "AUDITOR", "USER", "VIEWER"})
    public Result<Report> getReport(@PathVariable Long id) {
        Report report = reportService.getReportById(id);
        return Result.success(report);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "UPDATE_REPORT", targetType = "REPORT")
    public Result<Report> updateReport(@PathVariable Long id, @RequestBody ReportService.UpdateReportRequest request) {
        Report report = reportService.updateReport(id, request);
        return Result.success(report);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "DELETE_REPORT", targetType = "REPORT")
    public Result<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/submit")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "SUBMIT_REVIEW", targetType = "REPORT")
    public Result<Report> submitForReview(@PathVariable Long id, @RequestBody ReportService.SubmitReviewRequest request) {
        Report report = reportService.submitForReview(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/approve")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "APPROVE_REPORT", targetType = "REPORT")
    public Result<Report> approveReport(@PathVariable Long id, @RequestBody ReportService.ReviewRequest request) {
        Report report = reportService.approveReport(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/reject")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "REJECT_REPORT", targetType = "REPORT")
    public Result<Report> rejectReport(@PathVariable Long id, @RequestBody ReportService.ReviewRequest request) {
        Report report = reportService.rejectReport(id, request);
        return Result.success(report);
    }

    @PostMapping("/{id}/publish")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "PUBLISH_REPORT", targetType = "REPORT")
    public Result<Report> publishReport(@PathVariable Long id) {
        Report report = reportService.publishReport(id);
        return Result.success(report);
    }
}
