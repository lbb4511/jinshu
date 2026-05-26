package com.jinshu.common.entity;

import lombok.Data;

import java.util.Map;

@Data
public class ExportConfig {

    private Long reportId;

    private String format;

    private Map<String, Object> filters;

    private String outputPath;

    private Integer estimatedRows;
}
