package com.jinshu.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tenant {

    private Long id;
    private String name;
    private String code;
    private String status;
    private String quotaConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
