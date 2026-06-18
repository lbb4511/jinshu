package com.jinshu.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileNameUtil 文件命名工具测试")
class FileNameUtilTest {

    @Test
    @DisplayName("generateExportFileName: 返回 .xlsx 后缀")
    void given_excelFormat_when_generateFileName_then_xlsx() {
        String name = FileNameUtil.generateExportFileName(1L, 100L, "EXCEL");
        assertThat(name).endsWith(".xlsx");
        assertThat(name).startsWith("1_100_");
    }

    @Test
    @DisplayName("generateExportFileName: 返回 .csv 后缀")
    void given_csvFormat_when_generateFileName_then_csv() {
        String name = FileNameUtil.generateExportFileName(1L, 100L, "CSV");
        assertThat(name).endsWith(".csv");
    }

    @Test
    @DisplayName("generateExportFilePath: 包含 /data/output/ 前缀")
    void given_excelFormat_when_generateFilePath_then_correctPrefix() {
        String path = FileNameUtil.generateExportFilePath(1L, 100L, "EXCEL");
        assertThat(path).startsWith("/data/jinshu/output/1/");
        assertThat(path).endsWith(".xlsx");
    }

    @Test
    @DisplayName("getSafeFileName: 替换非法字符为下划线")
    void given_filenameWithIllegalChars_when_getSafe_then_replaced() {
        assertThat(FileNameUtil.getSafeFileName("a/b:c*d?")).isEqualTo("a_b_c_d_");
    }

    @Test
    @DisplayName("getSafeFileName: null 输入返回默认值")
    void given_null_when_getSafe_then_default() {
        assertThat(FileNameUtil.getSafeFileName(null)).isEqualTo("export");
    }

    @Test
    @DisplayName("getSafeFileName: 合法文件名不变")
    void given_cleanName_when_getSafe_then_unchanged() {
        assertThat(FileNameUtil.getSafeFileName("report_2026")).isEqualTo("report_2026");
    }
}
