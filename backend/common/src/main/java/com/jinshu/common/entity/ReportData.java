package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportData {

    private Long id;

    private Long tenantId;

    private Long reportId;

    private Integer rowNo;

    private String dataJson;

    private LocalDateTime createdAt;
}
