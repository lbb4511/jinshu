package com.jinshu.batch.model;

import lombok.Data;

import java.util.List;

@Data
public class ColumnSchema {

    private String name;

    private String displayName;

    private ColumnType type;

    private boolean required;

    private boolean unique;

    private Integer maxLength;

    private String dateFormat;

    private List<String> allowedValues;

    private String description;
}
