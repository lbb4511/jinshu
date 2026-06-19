package com.jinshu.batch.processor;

import com.jinshu.batch.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ReportRowValidator {

    private final List<ColumnSchema> columns;

    private final List<BusinessRule> businessRules;

    private final Set<String> uniqueValueCache;

    public ReportRowValidator(List<ColumnSchema> columns,
                              List<BusinessRule> businessRules,
                              Set<String> uniqueValueCache) {
        this.columns = columns;
        this.businessRules = businessRules;
        this.uniqueValueCache = uniqueValueCache;
    }

    public ValidationResult validate(ExcelImportRow row) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, Object> cells = row.getCells();

        validateL1Format(cells, errors);
        validateL2Type(cells, errors);
        validateL3Business(row, cells, errors);

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    private void validateL1Format(Map<String, Object> cells, List<ValidationError> errors) {
        if (columns == null) {
            return;
        }
        for (ColumnSchema col : columns) {
            Object value = cells.get(col.getName());

            if (col.isRequired() && isEmpty(value)) {
                errors.add(new ValidationError(0, col.getName(), "REQUIRED", "必填列为空", value));
                continue;
            }

            if (isEmpty(value)) continue;

            if (col.getMaxLength() != null && value.toString().length() > col.getMaxLength()) {
                errors.add(new ValidationError(0, col.getName(), "FORMAT",
                        "超出最大长度 " + col.getMaxLength(), value));
            }

            if (col.getAllowedValues() != null && !col.getAllowedValues().isEmpty()) {
                if (!col.getAllowedValues().contains(value.toString())) {
                    errors.add(new ValidationError(0, col.getName(), "FORMAT",
                            "值不在允许范围内: " + col.getAllowedValues(), value));
                }
            }
        }
    }

    private void validateL2Type(Map<String, Object> cells, List<ValidationError> errors) {
        if (columns == null) {
            return;
        }
        for (ColumnSchema col : columns) {
            Object value = cells.get(col.getName());
            if (isEmpty(value)) continue;

            if (!DataTypeConverter.matchesType(value, col.getType(), col.getDateFormat())) {
                errors.add(new ValidationError(0, col.getName(), "TYPE",
                        "类型不匹配，期望 " + col.getType(), value));
            }
        }
    }

    private void validateL3Business(ExcelImportRow row, Map<String, Object> cells,
                                    List<ValidationError> errors) {
        if (businessRules != null) {
            for (BusinessRule rule : businessRules) {
                if (!rule.validate(cells)) {
                    errors.add(new ValidationError(0, rule.getColumn(), "BUSINESS",
                            rule.getMessage(), cells.get(rule.getColumn())));
                }
            }
        }

        if (columns == null) {
            return;
        }
        for (ColumnSchema col : columns) {
            if (col.isUnique()) {
                Object value = cells.get(col.getName());
                if (value != null) {
                    String cacheKey = col.getName() + ":" + value;
                    if (uniqueValueCache.contains(cacheKey)) {
                        errors.add(new ValidationError(0, col.getName(), "BUSINESS",
                                "违反唯一性约束", value));
                    }
                    uniqueValueCache.add(cacheKey);
                }
            }
        }
    }

    private boolean isEmpty(Object value) {
        return value == null || (value instanceof String && ((String) value).trim().isEmpty());
    }
}
