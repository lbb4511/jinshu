package com.jinshu.batch.model;

public enum ColumnType {
    TEXT,
    NUMBER,
    DATE,
    DATETIME;

    public static ColumnType fromString(String type) {
        if (type == null) return TEXT;
        return switch (type.toUpperCase()) {
            case "NUMBER", "INTEGER", "DECIMAL", "FLOAT", "DOUBLE", "LONG" -> NUMBER;
            case "DATE" -> DATE;
            case "DATETIME", "TIMESTAMP" -> DATETIME;
            default -> TEXT;
        };
    }
}
