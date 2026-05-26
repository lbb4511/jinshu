package com.jinshu.api.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.AuditLogMapper;
import com.jinshu.common.audit.AuditLogEvent;
import com.jinshu.common.entity.AuditLogEntry;
import com.jinshu.common.entity.AuditRootHash;
import com.jinshu.common.utils.HashUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuditLogPersistenceService {

    private static final int BATCH_SIZE = 100;
    private static final long ROOT_HASH_INTERVAL_MINUTES = 60;

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Thread workerThread;
    private volatile boolean running = true;

    public AuditLogPersistenceService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.workerThread = new Thread(this::processQueue, "audit-log-worker");
        this.workerThread.setDaemon(true);
    }

    @PostConstruct
    public void start() {
        workerThread.start();
        log.info("Audit log persistence worker started");
    }

    @PreDestroy
    public void stop() {
        running = false;
        workerThread.interrupt();
    }

    private void processQueue() {
        List<AuditLogEvent> batch = new ArrayList<>(BATCH_SIZE);
        Map<Long, String> latestHashCache = new HashMap<>();

        while (running) {
            try {
                AuditLogEvent event = AuditLogger.getQueue().poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    batch.add(event);
                }

                if (batch.size() >= BATCH_SIZE) {
                    flushBatch(batch, latestHashCache);
                    batch.clear();
                }

                if (event == null && !batch.isEmpty()) {
                    flushBatch(batch, latestHashCache);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Audit log persistence error", e);
            }
        }

        if (!batch.isEmpty()) {
            flushBatch(batch, latestHashCache);
        }
    }

    private void flushBatch(List<AuditLogEvent> events, Map<Long, String> latestHashCache) {
        Map<Long, List<AuditLogEntry>> entriesByTenant = new HashMap<>();

        for (AuditLogEvent event : events) {
            Long tenantId = event.getTenantId();
            if (tenantId == null) continue;

            String previousHash = latestHashCache.computeIfAbsent(tenantId, this::getLatestHash);
            String logHash = computeHash(previousHash, event);

            AuditLogEntry entry = toEntry(event, previousHash, logHash);
            entriesByTenant.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(entry);

            latestHashCache.put(tenantId, logHash);
        }

        for (Map.Entry<Long, List<AuditLogEntry>> tenantEntry : entriesByTenant.entrySet()) {
            Long tenantId = tenantEntry.getKey();
            List<AuditLogEntry> entries = tenantEntry.getValue();

            for (AuditLogEntry entry : entries) {
                try {
                    auditLogMapper.insert(entry);
                } catch (Exception e) {
                    log.error("Failed to insert audit log entry for tenant {}: {}", tenantId, e.getMessage());
                }
            }

            AuditLogEntry lastEntry = entries.get(entries.size() - 1);
            updateRootHashIfNeeded(tenantId, lastEntry);
        }

        log.debug("Flushed {} audit log entries", events.size());
    }

    private void updateRootHashIfNeeded(Long tenantId, AuditLogEntry entry) {
        LocalDateTime entryTime = entry.getCreatedAt();
        LocalDateTime hourStart = entryTime.truncatedTo(ChronoUnit.HOURS);

        if (entryTime.getMinute() == 59 && entryTime.getSecond() >= 55) {
            try {
                long logCount = auditLogMapper.countByTenantAndTime(tenantId, hourStart);
                AuditRootHash existing = auditLogMapper.selectRootHash(tenantId, hourStart);

                if (existing == null || !existing.getRootHash().equals(entry.getLogHash())) {
                    AuditRootHash rootHash = new AuditRootHash();
                    rootHash.setTenantId(tenantId);
                    rootHash.setHourStart(hourStart);
                    rootHash.setRootHash(entry.getLogHash());
                    rootHash.setLogCount((int) logCount);
                    rootHash.setCreatedAt(LocalDateTime.now());
                    auditLogMapper.insertRootHash(rootHash);
                    log.info("Audit root hash updated: tenant={}, hour={}, hash={}",
                        tenantId, hourStart, entry.getLogHash());
                }
            } catch (Exception e) {
                log.warn("Failed to update root hash for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    private String getLatestHash(Long tenantId) {
        try {
            String hash = auditLogMapper.selectLatestLogHash(tenantId);
            return hash != null ? hash : "0".repeat(64);
        } catch (Exception e) {
            log.warn("Failed to get latest hash for tenant {}: {}", tenantId, e.getMessage());
            return "0".repeat(64);
        }
    }

    private String computeHash(String previousHash, AuditLogEvent event) {
        String input = previousHash + "|" +
                event.getTenantId() + "|" +
                event.getUserId() + "|" +
                event.getOperation() + "|" +
                event.getStatus() + "|" +
                (event.getCreatedAt() != null ? event.getCreatedAt().toString() : "");
        return HashUtils.sha256(input);
    }

    private AuditLogEntry toEntry(AuditLogEvent event, String previousHash, String logHash) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setTenantId(event.getTenantId());
        entry.setUserId(event.getUserId());
        entry.setUsername(event.getUsername());
        entry.setOperation(event.getOperation());
        entry.setTargetType(event.getTargetType());
        entry.setTargetId(event.getTargetId());
        entry.setTargetName(event.getTargetName());
        entry.setIpAddress(event.getIpAddress());
        entry.setUserAgent(event.getUserAgent());
        entry.setRequestParams(serializeParams(event.getRequestParams()));
        entry.setStatus(event.getStatus());
        entry.setErrorMessage(event.getErrorMessage());
        entry.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now());
        entry.setLogHash(logHash);
        entry.setPreviousHash(previousHash);
        return entry;
    }

    private String serializeParams(String params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        if (params.length() > 2048) {
            return params.substring(0, 2048);
        }
        return params;
    }
}
