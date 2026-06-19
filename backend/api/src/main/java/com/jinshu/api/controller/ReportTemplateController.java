package com.jinshu.api.controller;

import com.jinshu.api.service.ReportTemplateService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.ReportTemplate;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireOwner;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    @PostMapping
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "CREATE_REPORT_TEMPLATE", targetType = "REPORT_TEMPLATE")
    public Result<ReportTemplate> createTemplate(@RequestBody ReportTemplateService.CreateTemplateRequest request) {
        ReportTemplate template = reportTemplateService.createTemplate(request);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "UPDATE_REPORT_TEMPLATE", targetType = "REPORT_TEMPLATE")
    public Result<ReportTemplate> updateTemplate(@PathVariable Long id,
                                                 @RequestBody ReportTemplateService.UpdateTemplateRequest request) {
        ReportTemplate template = reportTemplateService.updateTemplate(id, request);
        return Result.success(template);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "DELETE_REPORT_TEMPLATE", targetType = "REPORT_TEMPLATE")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        reportTemplateService.deleteTemplate(id);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "AUDITOR", "USER", "VIEWER"})
    public Result<ReportTemplate> getTemplate(@PathVariable Long id) {
        ReportTemplate template = reportTemplateService.getTemplate(id);
        return Result.success(template);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "AUDITOR", "USER", "VIEWER"})
    public Result<PageResult<ReportTemplate>> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includePublic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ReportTemplate> result = reportTemplateService.listTemplates(category, keyword, includePublic, page, pageSize);
        return Result.success(result);
    }

    @PostMapping("/{id}/apply")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "APPLY_REPORT_TEMPLATE", targetType = "REPORT_TEMPLATE")
    public Result<Long> applyTemplate(@PathVariable Long id,
                                      @RequestBody ReportTemplateService.ApplyTemplateRequest request) {
        Long reportId = reportTemplateService.applyTemplate(id, request);
        return Result.success(reportId);
    }
}
