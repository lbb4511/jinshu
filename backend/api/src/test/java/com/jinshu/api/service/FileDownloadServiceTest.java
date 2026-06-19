package com.jinshu.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FileDownloadService 文件下载服务测试")
@ExtendWith(MockitoExtension.class)
class FileDownloadServiceTest {

    @Mock
    private TaskMapper taskMapper;

    private FileDownloadService fileDownloadService;
    private ObjectMapper objectMapper;

    private static final Long TENANT_ID = 100L;
    private static final Long TASK_ID = 300L;
    private static Path testBasePath;

    @BeforeAll
    static void initBasePath() throws Exception {
        testBasePath = Files.createTempDirectory("jinshu_test_base_");
        System.setProperty("jinshu.file.base-path", testBasePath.toString());
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fileDownloadService = new FileDownloadService(taskMapper, objectMapper);
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Path createTestFile(String relativePath, byte[] content) throws Exception {
        Path file = testBasePath.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        return file;
    }

    @Test
    @DisplayName("resolveDownloadFile: 通过 result_file_path 成功解析文件")
    void given_taskWithResultFilePath_when_resolve_then_returnDownloadFile() throws Exception {
        Path tempFile = createTestFile("output/100/report.xlsx", "testdata".getBytes());

        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("SUCCESS");
        task.setResultFilePath(tempFile.toString());
        task.setResultFileName("report.xlsx");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        FileDownloadService.DownloadFile result = fileDownloadService.resolveDownloadFile(TASK_ID);

        assertThat(result.path()).isEqualTo(tempFile.toAbsolutePath().normalize());
        assertThat(result.size()).isEqualTo(8L);
        assertThat(result.fileName()).isEqualTo("report.xlsx");
    }

    @Test
    @DisplayName("resolveDownloadFile: 旧任务从 parameters 解析 outputPath")
    void given_legacyTaskWithOutputPath_when_resolve_then_returnDownloadFile() throws Exception {
        Path tempFile = createTestFile("output/100/legacy.xlsx", "legacydata".getBytes());

        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("SUCCESS");
        task.setResultFilePath(null);
        task.setParameters("{\"outputPath\":\"" + tempFile.toString().replace("\\", "/") + "\"}");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        FileDownloadService.DownloadFile result = fileDownloadService.resolveDownloadFile(TASK_ID);

        assertThat(result.size()).isEqualTo(10L);
        assertThat(result.fileName()).isEqualTo("legacy.xlsx");
    }

    @Test
    @DisplayName("resolveDownloadFile: 任务不存在抛 TASK_NOT_FOUND")
    void given_taskNotFound_when_resolve_then_throw() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(null);

        assertThatThrownBy(() -> fileDownloadService.resolveDownloadFile(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("resolveDownloadFile: 跨租户任务抛 TASK_NOT_FOUND")
    void given_otherTenantTask_when_resolve_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(999L);
        task.setStatus("SUCCESS");
        task.setResultFilePath(testBasePath.resolve("output/100/file.xlsx").toString());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> fileDownloadService.resolveDownloadFile(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("resolveDownloadFile: 未完成任务抛 TASK_STATUS_ERROR")
    void given_processingTask_when_resolve_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("PROCESSING");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> fileDownloadService.resolveDownloadFile(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.TASK_STATUS_ERROR.getCode()));
    }

    @Test
    @DisplayName("resolveDownloadFile: 路径逃逸抛 PARAM_ERROR")
    void given_pathTraversal_when_resolve_then_throw() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setStatus("SUCCESS");
        task.setResultFilePath("/etc/passwd");
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> fileDownloadService.resolveDownloadFile(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }
}
