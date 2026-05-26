package com.jinshu.batch.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExcelImportRow {

    private int rowNo;

    private Map<String, Object> cells = new LinkedHashMap<>();

    private boolean valid = true;

    public void addCell(String columnName, Object value) {
        cells.put(columnName, value);
    }

    public Object getCellValue(String columnName) {
        return cells.get(columnName);
    }
}
