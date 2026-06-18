package com.jinshu.api.controller;

import com.jinshu.api.service.AuditQueryService;
import com.jinshu.common.entity.AuditLogEntry;
import com.jinshu.common.entity.AuditRootHash;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/logs")
    public Result<PageResult<AuditLogEntry>> queryLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(auditQueryService.queryAuditLogs(userId, operation, targetType, status, dateFrom, dateTo, page, pageSize));
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        String csv = auditQueryService.exportCsv(userId, operation, targetType, status, dateFrom, dateTo);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=utf-8"));
        headers.setContentDispositionFormData("attachment", "audit_logs.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/integrity")
    public Result<AuditQueryService.IntegrityCheckResult> verifyIntegrity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hourStart) {
        return Result.success(auditQueryService.verifyIntegrity(hourStart));
    }

    @GetMapping("/root-hash")
    public Result<List<AuditRootHash>> listRootHashes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        return Result.success(auditQueryService.listRootHashes(dateFrom, dateTo));
    }
}
