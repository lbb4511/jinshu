package com.jinshu.worker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.worker.dao.ReportDataMapper;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelExportHandler - Excel 导出处理器")
class ExcelExportHandlerTest {

    @Mock
    private ExportProgressTracker progressTracker;

    @Mock
    private ReportDataMapper reportDataMapper;

    private ExcelExportHandler excelExportHandler;

    private Path tempDir;

    private static final Long TENANT_ID = 1L;
    private static final Long REPORT_ID = 100L;
    private static final Long TASK_ID = 999L;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        excelExportHandler = new ExcelExportHandler(progressTracker, reportDataMapper, objectMapper);
        tempDir = Files.createTempDirectory("excel-export-test");
        TenantContext.setTenantId(TENANT_ID);

        lenient().when(progressTracker.getTotalRows(anyLong())).thenReturn(2L);
        lenient().when(reportDataMapper.countByReportId(anyLong(), anyLong())).thenReturn(2L);
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    @DisplayName("无模板导出：使用默认表头并写入报表数据")
    void given_noTemplate_when_export_then_writeDefaultHeaderAndData() throws Exception {
        mockReportData(twoRows());
        String outputPath = tempDir.resolve("no-template.xlsx").toString();

        excelExportHandler.export(TASK_ID, REPORT_ID, outputPath, null);

        assertThat(Path.of(outputPath)).exists();
        List<List<Object>> sheetData = readSheet(outputPath, 0);
        assertThat(sheetData).hasSize(3);
        assertThat(sheetData.get(0)).containsExactly("列1", "列2", "列3");
    }

    @Test
    @DisplayName("有模板导出：保留模板表头并追加数据")
    void given_template_when_export_then_preserveHeaderAndAppendData() throws Exception {
        Path templatePath = createTemplateFile();
        mockReportData(twoRows());
        String outputPath = tempDir.resolve("with-template.xlsx").toString();

        excelExportHandler.export(TASK_ID, REPORT_ID, outputPath, templatePath.toString());

        assertThat(Path.of(outputPath)).exists();
        List<List<Object>> sheetData = readSheet(outputPath, 0);
        assertThat(sheetData).hasSize(3);
        assertThat(sheetData.get(0)).containsExactly("编号", "名称", "数值");
    }

    private Path createTemplateFile() throws Exception {
        Path templatePath = tempDir.resolve("template.xlsx");
        com.alibaba.excel.EasyExcel.write(templatePath.toString())
                .sheet("Sheet1")
                .doWrite(java.util.List.of(java.util.List.of("编号", "名称", "数值")));
        return templatePath;
    }

    private void mockReportData(java.util.List<Map<String, Object>> rows) {
        doAnswer(invocation -> {
            ResultHandler<Map<String, Object>> handler = invocation.getArgument(2);
            for (Map<String, Object> row : rows) {
                handler.handleResult(new org.apache.ibatis.session.ResultContext<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> getResultObject() {
                        return row;
                    }

                    @Override
                    public int getResultCount() {
                        return 0;
                    }

                    @Override
                    public boolean isStopped() {
                        return false;
                    }

                    @Override
                    public void stop() {
                    }
                });
            }
            return null;
        }).when(reportDataMapper).selectByReportId(anyLong(), anyLong(), any());
    }

    private java.util.List<Map<String, Object>> twoRows() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("row_no", 1);
        row1.put("data_json_text", "{\"name\":\"A\",\"value\":10}");

        Map<String, Object> row2 = new HashMap<>();
        row2.put("row_no", 2);
        row2.put("data_json_text", "{\"name\":\"B\",\"value\":20}");

        return java.util.List.of(row1, row2);
    }

    private List<List<Object>> readSheet(String path, int sheetNo) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(path)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(sheetNo);
            List<List<Object>> rows = new java.util.ArrayList<>();
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                List<Object> values = new java.util.ArrayList<>();
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    values.add(readCellValue(cell));
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private Object readCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
