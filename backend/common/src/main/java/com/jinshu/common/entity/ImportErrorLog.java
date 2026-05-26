package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImportErrorLog {

    private Long id;

    private Long tenantId;

    private Long taskId;

    private Long reportId;

    private Integer rowNo;

    private String columnName;

    private String errorType;

    private String errorMessage;

    private String cellValue;

    private LocalDateTime createdAt;
}
