package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportWorkflow {

    private Long id;

    private Long reportId;

    private Long tenantId;

    private String status;

    private Long reviewedBy;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private String operation;

    private Long operatorId;

    private LocalDateTime createdAt;
}
