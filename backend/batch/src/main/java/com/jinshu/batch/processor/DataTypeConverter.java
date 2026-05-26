package com.jinshu.batch.processor;

import com.jinshu.batch.model.ColumnType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
public class DataTypeConverter {

    private static final List<String> COMMON_DATE_FORMATS = List.of(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyyMMdd",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
    );

    public static Object convert(Object value, ColumnType targetType, String dateFormat) {
        if (value == null) return null;
        if (targetType == ColumnType.TEXT) return value.toString();

        return switch (targetType) {
            case NUMBER -> convertToNumber(value);
            case DATE -> convertToDate(value, dateFormat);
            case DATETIME -> convertToDateTime(value, dateFormat);
            default -> value.toString();
        };
    }

    public static boolean matchesType(Object value, ColumnType targetType, String dateFormat) {
        if (value == null) return true;
        try {
            convert(value, targetType, dateFormat);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Object convertToNumber(Object value) {
        if (value instanceof Number) return value;
        String str = value.toString().trim().replace(",", "");
        if (str.isEmpty()) return null;
        return new BigDecimal(str);
    }

    private static Object convertToDate(Object value, String dateFormat) {
        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        if (dateFormat != null && !dateFormat.isEmpty()) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(dateFormat));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期格式不匹配: " + dateFormat);
            }
        }

        for (String fmt : COMMON_DATE_FORMATS) {
            try {
                if (fmt.contains("HH:mm:ss")) {
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(fmt));
                }
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("无法解析日期: " + str);
    }

    private static Object convertToDateTime(Object value, String dateFormat) {
        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        if (dateFormat != null && !dateFormat.isEmpty()) {
            try {
                return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(dateFormat));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期时间格式不匹配: " + dateFormat);
            }
        }

        for (String fmt : COMMON_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("无法解析日期时间: " + str);
    }
}
