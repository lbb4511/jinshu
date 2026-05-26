package com.jinshu.batch.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BusinessRule {

    private String name;

    private String column;

    private String relatedColumn;

    private RuleType type;

    private String message;

    private Map<String, Object> params;

    public enum RuleType {
        UNIQUE,
        DICT_VALUE,
        CROSS_FIELD_COMPARE,
        RANGE_CHECK
    }

    public boolean validate(Map<String, Object> row) {
        return switch (type) {
            case DICT_VALUE -> validateDictValue(row);
            case CROSS_FIELD_COMPARE -> validateCrossFieldCompare(row);
            case RANGE_CHECK -> validateRangeCheck(row);
            case UNIQUE -> true;
        };
    }

    private boolean validateDictValue(Map<String, Object> row) {
        Object value = row.get(column);
        if (value == null) return true;
        if (params == null || !params.containsKey("allowedValues")) return true;
        @SuppressWarnings("unchecked")
        List<String> allowed = (List<String>) params.get("allowedValues");
        return allowed.contains(value.toString());
    }

    private boolean validateCrossFieldCompare(Map<String, Object> row) {
        Object value = row.get(column);
        Object relatedValue = row.get(relatedColumn);
        if (value == null || relatedValue == null) return true;
        if (params != null && "gt".equals(params.get("operator"))) {
            return compareAsNumber(value, relatedValue) > 0;
        }
        if (params != null && "gte".equals(params.get("operator"))) {
            return compareAsNumber(value, relatedValue) >= 0;
        }
        return true;
    }

    private boolean validateRangeCheck(Map<String, Object> row) {
        Object value = row.get(column);
        if (value == null) return true;
        if (params == null) return true;
        double num = toDouble(value);
        if (params.containsKey("min") && num < toDouble(params.get("min"))) return false;
        if (params.containsKey("max") && num > toDouble(params.get("max"))) return false;
        return true;
    }

    private double compareAsNumber(Object a, Object b) {
        return toDouble(a) - toDouble(b);
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
