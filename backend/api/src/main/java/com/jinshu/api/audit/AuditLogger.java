package com.jinshu.api.audit;

import com.jinshu.common.audit.AuditLogEvent;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class AuditLogger {

    private static final BlockingQueue<AuditLogEvent> QUEUE = new LinkedBlockingQueue<>(10000);

    public static void log(String operation, String targetType, Long targetId, String targetName,
                           String status, String errorMessage) {
        try {
            AuditLogEvent event = AuditLogEvent.builder()
                    .tenantId(TenantContext.getTenantId())
                    .userId(UserContext.getUserId())
                    .username(UserContext.getUsername())
                    .operation(operation)
                    .targetType(targetType)
                    .targetId(targetId)
                    .targetName(targetName)
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            boolean offered = QUEUE.offer(event);
            if (!offered) {
                log.warn("Audit log queue is full, event dropped: {}", operation);
            }
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    public static void logLogin(Long userId, String username, String status, String errorMessage) {
        log("LOGIN", "USER", userId, username, status, errorMessage);
    }

    public static void logLogout(Long userId, String username, String status, String errorMessage) {
        log("LOGOUT", "USER", userId, username, status, errorMessage);
    }

    public static BlockingQueue<AuditLogEvent> getQueue() {
        return QUEUE;
    }
}
