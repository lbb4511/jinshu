package com.jinshu.api.service;

import com.jinshu.api.dao.AuditLogMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.entity.AuditLogEntry;
import com.jinshu.common.entity.AuditRootHash;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private static final int MAX_DATE_RANGE_DAYS = 90;

    private final AuditLogMapper auditLogMapper;

    public PageResult<AuditLogEntry> queryAuditLogs(Long userId, String operation, String targetType,
                                                     String status, LocalDateTime dateFrom, LocalDateTime dateTo,
                                                     int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();

        if (dateFrom != null && dateTo != null) {
            if (java.time.Duration.between(dateFrom, dateTo).toDays() > MAX_DATE_RANGE_DAYS) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "查询时间范围不能超过90天");
            }
        }

        if (pageSize > 100) {
            pageSize = 100;
        }

        int offset = (page - 1) * pageSize;
        List<AuditLogEntry> list = auditLogMapper.selectList(tenantId, userId, operation, targetType, status, dateFrom, dateTo, offset, pageSize);
        long total = auditLogMapper.countList(tenantId, userId, operation, targetType, status, dateFrom, dateTo);
        return PageResult.of(list, total, page, pageSize);
    }

    public IntegrityCheckResult verifyIntegrity(LocalDateTime hourStart) {
        Long tenantId = TenantContext.getTenantId();
        if (hourStart == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请指定校验的小时起始时间");
        }

        LocalDateTime hourStartTrunc = hourStart.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime hourEnd = hourStartTrunc.plusHours(1);

        AuditRootHash stored = auditLogMapper.selectRootHash(tenantId, hourStartTrunc);
        if (stored == null) {
            return new IntegrityCheckResult(null, null, null, 0, false, "该小时无根哈希记录");
        }

        List<AuditLogEntry> logs = auditLogMapper.selectByHour(tenantId, hourStartTrunc, hourEnd);
        String computedRoot = computeMerkleRoot(logs);
        boolean consistent = stored.getRootHash().equals(computedRoot);

        return new IntegrityCheckResult(
            tenantId, hourStartTrunc, hourEnd,
            logs.size(), consistent,
            consistent ? "审计日志完整，未发现篡改" : "审计日志可能被篡改！根哈希不一致"
        );
    }

    public List<AuditRootHash> listRootHashes(LocalDateTime dateFrom, LocalDateTime dateTo) {
        Long tenantId = TenantContext.getTenantId();
        return auditLogMapper.selectRootHashList(tenantId, dateFrom, dateTo);
    }

    public String exportCsv(Long userId, String operation, String targetType,
                             String status, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Long tenantId = TenantContext.getTenantId();

        List<AuditLogEntry> logs = auditLogMapper.selectList(tenantId, userId, operation, targetType, status,
            dateFrom, dateTo, 0, 100000);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,租户ID,用户ID,用户名,操作,目标类型,目标ID,目标名称,IP地址,状态,错误信息,日志哈希,前一哈希,创建时间\n");
        for (AuditLogEntry log : logs) {
            sb.append(log.getId()).append(",")
                .append(log.getTenantId()).append(",")
                .append(log.getUserId()).append(",")
                .append(escapeCsv(log.getUsername())).append(",")
                .append(escapeCsv(log.getOperation())).append(",")
                .append(escapeCsv(log.getTargetType())).append(",")
                .append(log.getTargetId()).append(",")
                .append(escapeCsv(log.getTargetName())).append(",")
                .append(escapeCsv(log.getIpAddress())).append(",")
                .append(log.getStatus()).append(",")
                .append(escapeCsv(log.getErrorMessage())).append(",")
                .append(log.getLogHash()).append(",")
                .append(log.getPreviousHash()).append(",")
                .append(log.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }

    private String computeMerkleRoot(List<AuditLogEntry> logs) {
        if (logs == null || logs.isEmpty()) return "";
        List<String> currentLevel = new ArrayList<>();
        for (AuditLogEntry log : logs) {
            currentLevel.add(log.getLogHash() != null ? log.getLogHash() : "");
        }
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                if (i + 1 < currentLevel.size()) {
                    nextLevel.add(HashUtils.sha256(currentLevel.get(i) + currentLevel.get(i + 1)));
                } else {
                    nextLevel.add(currentLevel.get(i));
                }
            }
            currentLevel = nextLevel;
        }
        return currentLevel.isEmpty() ? "" : currentLevel.get(0);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public record IntegrityCheckResult(
        Long tenantId,
        LocalDateTime hourStart,
        LocalDateTime hourEnd,
        int logCount,
        boolean consistent,
        String message
    ) {}
}
