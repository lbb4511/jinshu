package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataSourceConnectionLog {

    private Long id;

    private Long tenantId;

    private Long dataSourceId;

    private Long testUserId;

    private Boolean success;

    private String errorMessage;

    private Integer responseTimeMs;

    private LocalDateTime createdAt;
}
