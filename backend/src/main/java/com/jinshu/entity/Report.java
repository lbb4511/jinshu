package com.jinshu.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Report {

    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
