package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRootHash {

    private Long id;

    private Long tenantId;

    private LocalDateTime hourStart;

    private String rootHash;

    private Integer logCount;

    private LocalDateTime createdAt;
}
