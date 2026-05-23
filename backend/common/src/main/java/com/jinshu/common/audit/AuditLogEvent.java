package com.jinshu.common.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogEvent {

    private Long tenantId;

    private Long userId;

    private String username;

    private String operation;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String ipAddress;

    private String userAgent;

    private String requestParams;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private String logHash;

    private String previousHash;
}
