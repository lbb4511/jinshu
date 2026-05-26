package com.jinshu.batch.processor;

import com.jinshu.batch.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportRowValidator 三级校验引擎测试")
class ReportRowValidatorTest {

    private List<ColumnSchema> columns;

    private List<BusinessRule> businessRules;

    private Set<String> uniqueCache;

    @BeforeEach
    void setUp() {
        columns = new ArrayList<>();
        businessRules = new ArrayList<>();
        uniqueCache = new HashSet<>();
    }

    private ColumnSchema column(String name, ColumnType type, boolean required) {
        ColumnSchema col = new ColumnSchema();
        col.setName(name);
        col.setType(type);
        col.setRequired(required);
        return col;
    }

    private ExcelImportRow row(int rowNo, Map<String, Object> cells) {
        ExcelImportRow row = new ExcelImportRow();
        row.setRowNo(rowNo);
        row.setCells(cells);
        return row;
    }

    // ============ L1 格式校验 ============

    @Test
    @DisplayName("L1: 必填列为空返回 REQUIRED 错误")
    void given_requiredFieldEmpty_when_validate_then_requiredError() {
        columns.add(column("name", ColumnType.TEXT, true));
        columns.add(column("age", ColumnType.NUMBER, false));

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("name", null);
        cells.put("age", 25);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("REQUIRED");
        assertThat(result.getErrors().get(0).getColumnName()).isEqualTo("name");
    }

    @Test
    @DisplayName("L1: 所有必填列有值则格式校验通过")
    void given_allRequiredFieldsPresent_when_validate_then_noFormatErrors() {
        columns.add(column("name", ColumnType.TEXT, true));
        columns.add(column("age", ColumnType.NUMBER, true));

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("name", "张三");
        cells.put("age", 30);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("L1: 超过最大长度返回 FORMAT 错误")
    void given_valueExceedsMaxLength_when_validate_then_formatError() {
        ColumnSchema col = column("remark", ColumnType.TEXT, false);
        col.setMaxLength(5);
        columns.add(col);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("remark", "太长了的文本内容");
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("FORMAT");
    }

    @Test
    @DisplayName("L1: 值不在允许范围内返回 FORMAT 错误")
    void given_valueNotInAllowedList_when_validate_then_formatError() {
        ColumnSchema col = column("status", ColumnType.TEXT, false);
        col.setAllowedValues(List.of("ACTIVE", "INACTIVE"));
        columns.add(col);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("status", "UNKNOWN");
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("FORMAT");
    }

    // ============ L2 类型校验 ============

    @Test
    @DisplayName("L2: 数字列传入非数字值返回 TYPE 错误")
    void given_nonNumericInNumericColumn_when_validate_then_typeError() {
        columns.add(column("age", ColumnType.NUMBER, false));

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("age", "abc");
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("TYPE");
    }

    @Test
    @DisplayName("L2: 数字列传入合法数字值通过")
    void given_validNumber_when_validate_then_typeOk() {
        columns.add(column("age", ColumnType.NUMBER, false));

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("age", 25);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("L2: 日期列传入非法日期返回 TYPE 错误")
    void given_invalidDateInDateColumn_when_validate_then_typeError() {
        columns.add(column("birth", ColumnType.DATE, false));

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("birth", "not-a-date");
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("TYPE");
    }

    // ============ L3 业务校验 ============

    @Test
    @DisplayName("L3: 字典值校验不通过返回 BUSINESS 错误")
    void given_dictValueNotAllowed_when_validate_then_businessError() {
        BusinessRule rule = new BusinessRule();
        rule.setName("status_dict");
        rule.setColumn("status");
        rule.setType(BusinessRule.RuleType.DICT_VALUE);
        rule.setMessage("状态值不在允许范围内");
        rule.setParams(Map.of("allowedValues", List.of("ACTIVE", "INACTIVE")));
        businessRules.add(rule);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("status", "PENDING");
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("BUSINESS");
    }

    @Test
    @DisplayName("L3: 唯一性约束重复值返回 BUSINESS 错误")
    void given_duplicateUniqueColumn_when_validate_then_businessError() {
        ColumnSchema col = column("code", ColumnType.TEXT, false);
        col.setUnique(true);
        columns.add(col);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);

        Map<String, Object> row1 = new HashMap<>();
        row1.put("code", "A001");
        assertThat(validator.validate(row(1, row1)).isValid()).isTrue();

        Map<String, Object> row2 = new HashMap<>();
        row2.put("code", "A001");
        ValidationResult result = validator.validate(row(2, row2));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("BUSINESS");
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("唯一性约束");
    }

    @Test
    @DisplayName("L3: 跨列逻辑校验（开始 < 结束）")
    void given_endBeforeStart_when_validate_then_businessError() {
        BusinessRule rule = new BusinessRule();
        rule.setName("date_range");
        rule.setColumn("end_date");
        rule.setRelatedColumn("start_date");
        rule.setType(BusinessRule.RuleType.CROSS_FIELD_COMPARE);
        rule.setMessage("结束日期必须大于开始日期");
        rule.setParams(Map.of("operator", "gt"));
        businessRules.add(rule);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("start_date", 100);
        cells.put("end_date", 50);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("BUSINESS");
    }

    @Test
    @DisplayName("L3: 范围检查超出最大值返回 BUSINESS 错误")
    void given_valueExceedsMax_when_validate_then_businessError() {
        BusinessRule rule = new BusinessRule();
        rule.setName("age_range");
        rule.setColumn("age");
        rule.setType(BusinessRule.RuleType.RANGE_CHECK);
        rule.setMessage("年龄不能超过 150");
        rule.setParams(Map.of("min", 0, "max", 150));
        businessRules.add(rule);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("age", 200);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("BUSINESS");
    }

    @Test
    @DisplayName("全字段合法通过三级校验")
    void given_allValidFields_when_validate_then_success() {
        columns.add(column("name", ColumnType.TEXT, true));
        columns.add(column("age", ColumnType.NUMBER, true));

        BusinessRule rule = new BusinessRule();
        rule.setName("age_range");
        rule.setColumn("age");
        rule.setType(BusinessRule.RuleType.RANGE_CHECK);
        rule.setParams(Map.of("min", 0, "max", 150));
        businessRules.add(rule);

        ReportRowValidator validator = new ReportRowValidator(columns, businessRules, uniqueCache);
        Map<String, Object> cells = new HashMap<>();
        cells.put("name", "张三");
        cells.put("age", 25);
        ValidationResult result = validator.validate(row(1, cells));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }
}
