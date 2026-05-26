package com.jinshu.batch.processor;

import com.jinshu.batch.model.ColumnType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataTypeConverter 类型转换器测试")
class DataTypeConverterTest {

    @Test
    @DisplayName("convert: null 输入返回 null")
    void given_null_when_convert_then_null() {
        assertThat(DataTypeConverter.convert(null, ColumnType.TEXT, null)).isNull();
    }

    @Test
    @DisplayName("convert: TEXT 类型返回 toString")
    void given_numberAsText_when_convert_then_toString() {
        Object result = DataTypeConverter.convert(123, ColumnType.TEXT, null);
        assertThat(result).isEqualTo("123");
    }

    @Test
    @DisplayName("convert: NUMBER 类型返回 BigDecimal")
    void given_numericString_when_convertToNumber_then_bigDecimal() {
        Object result = DataTypeConverter.convert("123.45", ColumnType.NUMBER, null);
        assertThat(result).isInstanceOf(BigDecimal.class);
        assertThat(new BigDecimal("123.45")).isEqualByComparingTo((BigDecimal) result);
    }

    @Test
    @DisplayName("convert: NUMBER 含千分位逗号")
    void given_numberWithComma_when_convert_then_bigDecimal() {
        Object result = DataTypeConverter.convert("1,234.56", ColumnType.NUMBER, null);
        assertThat(result).isInstanceOf(BigDecimal.class);
    }

    @Test
    @DisplayName("convert: NUMBER 空字符串返回 null")
    void given_emptyString_when_convertToNumber_then_null() {
        assertThat(DataTypeConverter.convert("", ColumnType.NUMBER, null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "2026-01-15, yyyy-MM-dd",
            "2026/01/15, yyyy/MM/dd",
            "01/15/2026, MM/dd/yyyy"
    })
    @DisplayName("convert: DATE 多种格式解析成功")
    void given_dateStrings_when_convertToDate_then_success(String input, String format) {
        Object result = DataTypeConverter.convert(input, ColumnType.DATE, format);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("convert: DATE 自动格式探测")
    void given_dateString_when_convertWithoutFormat_then_autoDetect() {
        Object result = DataTypeConverter.convert("2026-01-15", ColumnType.DATE, null);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("convert: 无效日期抛异常")
    void given_invalidDate_when_convert_then_throw() {
        assertThatThrownBy(() -> DataTypeConverter.convert("not-a-date", ColumnType.DATE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("matchesType: 类型匹配返回 true")
    void given_matchingType_when_matchesType_then_true() {
        assertThat(DataTypeConverter.matchesType("123", ColumnType.NUMBER, null)).isTrue();
    }

    @Test
    @DisplayName("matchesType: 类型不匹配返回 false")
    void given_nonMatchingType_when_matchesType_then_false() {
        assertThat(DataTypeConverter.matchesType("abc", ColumnType.NUMBER, null)).isFalse();
    }

    @Test
    @DisplayName("matchesType: null 值跳过校验")
    void given_null_when_matchesType_then_true() {
        assertThat(DataTypeConverter.matchesType(null, ColumnType.NUMBER, null)).isTrue();
    }
}
