package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataSource {

    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private String connectionConfig;

    private String status;

    private LocalDateTime lastTestTime;

    private String lastTestResult;

    private String description;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
