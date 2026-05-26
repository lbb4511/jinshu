package com.jinshu.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationError {

    private int rowNo;

    private String columnName;

    private String errorType;

    private String errorMessage;

    private Object cellValue;

    public ValidationError(int rowNo, String columnName, String errorType, String errorMessage) {
        this.rowNo = rowNo;
        this.columnName = columnName;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }
}
