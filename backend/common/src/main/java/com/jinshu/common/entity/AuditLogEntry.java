package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogEntry {

    private Long id;

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

    private String logHash;

    private String previousHash;

    private LocalDateTime createdAt;
}
