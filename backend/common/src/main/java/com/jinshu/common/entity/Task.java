package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Task {

    private Long id;

    private Long tenantId;

    private Long reportId;

    private String taskType;

    private String status;

    private Integer priority;

    private String parameters;

    private Integer progress;

    private String result;

    private String errorMessage;

    private Long parentTaskId;

    private Integer shardSeq;

    private Integer shardTotal;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;
}
