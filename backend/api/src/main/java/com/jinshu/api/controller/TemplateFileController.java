package com.jinshu.api.controller;

import com.jinshu.api.service.TemplateFileService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.ExcelTemplateFile;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Excel 模板文件管理控制器
 */
@RestController
@RequestMapping("/excel-templates")
@RequiredArgsConstructor
public class TemplateFileController {

    private final TemplateFileService templateFileService;

    @PostMapping("/upload")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "UPLOAD_EXCEL_TEMPLATE", targetType = "EXCEL_TEMPLATE")
    public Result<Map<String, Object>> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long reportId) {
        ExcelTemplateFile record = templateFileService.uploadTemplate(reportId, file);
        return Result.success(Map.of(
                "id", record.getId(),
                "fileName", record.getFileName(),
                "filePath", record.getFilePath(),
                "fileSize", record.getFileSize()));
    }

    @GetMapping
    @RequireRole({"ADMIN", "USER"})
    public Result<PageResult<ExcelTemplateFile>> listTemplates(
            @RequestParam(required = false) Long reportId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(templateFileService.listTemplates(reportId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    @AuditLog(operation = "DELETE_EXCEL_TEMPLATE", targetType = "EXCEL_TEMPLATE")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateFileService.deleteTemplate(id);
        return Result.success();
    }
}
