package com.jinshu.api.service;

import com.jinshu.api.dao.ExcelTemplateFileMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.ExcelTemplateFile;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateFileService - Excel 模板文件服务测试")
class TemplateFileServiceTest {

    @Mock
    private ExcelTemplateFileMapper excelTemplateFileMapper;

    private TemplateFileService templateFileService;

    private Path tempBaseDir;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;
    private static final Long TEMPLATE_ID = 100L;

    @BeforeEach
    void setUp() throws Exception {
        templateFileService = new TemplateFileService(excelTemplateFileMapper);
        tempBaseDir = Files.createTempDirectory("jinshu-test");
        System.setProperty("jinshu.file.base-path", tempBaseDir.toString());

        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setRole("USER");
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        UserContext.clear();
        System.clearProperty("jinshu.file.base-path");
        if (tempBaseDir != null) {
            Files.walk(tempBaseDir)
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
    @DisplayName("uploadTemplate: 上传 .xlsx 模板并保存记录")
    void given_validExcelFile_when_upload_then_saveRecord() {
        MultipartFile file = new MockMultipartFile(
                "file", "report.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake-excel-content".getBytes());

        doAnswer(invocation -> {
            ExcelTemplateFile record = invocation.getArgument(0);
            record.setId(TEMPLATE_ID);
            return 1;
        }).when(excelTemplateFileMapper).insert(any(ExcelTemplateFile.class));

        ExcelTemplateFile result = templateFileService.uploadTemplate(10L, file);

        assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getReportId()).isEqualTo(10L);
        assertThat(result.getFileName()).isEqualTo("report.xlsx");
        assertThat(result.getFileSize()).isEqualTo(file.getSize());
        assertThat(result.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(Path.of(result.getFilePath())).exists();
    }

    @Test
    @DisplayName("uploadTemplate: 非 .xlsx 文件抛文件类型不支持异常")
    void given_invalidFileType_when_upload_then_throw() {
        MultipartFile file = new MockMultipartFile("file", "report.csv", "text/csv", "a,b,c".getBytes());

        assertThatThrownBy(() -> templateFileService.uploadTemplate(null, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode()));
    }

    @Test
    @DisplayName("deleteTemplate: 创建者可删除模板")
    void given_creator_when_delete_then_removeFileAndRecord() throws Exception {
        Path storedFile = tempBaseDir.resolve("templates/1/uuid.xlsx");
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, "content".getBytes());

        ExcelTemplateFile record = new ExcelTemplateFile();
        record.setId(TEMPLATE_ID);
        record.setTenantId(TENANT_ID);
        record.setFilePath(storedFile.toString());
        record.setCreatedBy(USER_ID);
        when(excelTemplateFileMapper.selectById(TEMPLATE_ID)).thenReturn(record);

        templateFileService.deleteTemplate(TEMPLATE_ID);

        assertThat(storedFile).doesNotExist();
        verify(excelTemplateFileMapper).deleteById(TEMPLATE_ID);
    }

    @Test
    @DisplayName("deleteTemplate: 非所有者且非 ADMIN 删除抛权限异常")
    void given_otherUser_when_delete_then_throw() {
        ExcelTemplateFile record = new ExcelTemplateFile();
        record.setId(TEMPLATE_ID);
        record.setTenantId(TENANT_ID);
        record.setFilePath("/tmp/uuid.xlsx");
        record.setCreatedBy(999L);
        when(excelTemplateFileMapper.selectById(TEMPLATE_ID)).thenReturn(record);

        assertThatThrownBy(() -> templateFileService.deleteTemplate(TEMPLATE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.NOT_RESOURCE_OWNER.getCode()));
    }
}
