package com.jinshu.api.service;

import com.jinshu.common.dao.ImportErrorLogMapper;
import com.jinshu.common.entity.ImportErrorLog;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ImportErrorLogService 导入异常行查询测试")
@ExtendWith(MockitoExtension.class)
class ImportErrorLogServiceTest {

    @Mock
    private ImportErrorLogMapper importErrorLogMapper;

    @Mock
    private TaskService taskService;

    private ImportErrorLogService importErrorLogService;

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;

    @BeforeEach
    void setUp() {
        importErrorLogService = new ImportErrorLogService(importErrorLogMapper, taskService);
    }

    @AfterEach
    void tearDown() {
    }

    private Task createImportTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setTaskType("IMPORT");
        task.setStatus("FAILED");
        return task;
    }

    @Test
    @DisplayName("listErrors: 分页查询全部异常行")
    void given_importTask_when_listErrors_then_returnPage() {
        when(taskService.getTaskById(TASK_ID)).thenReturn(createImportTask());
        when(importErrorLogMapper.selectByTaskId(TASK_ID, 0, 20)).thenReturn(List.of(new ImportErrorLog()));
        when(importErrorLogMapper.countByTaskId(TASK_ID)).thenReturn(1L);

        PageResult<ImportErrorLog> result = importErrorLogService.listErrors(TASK_ID, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPageNum()).isEqualTo(1);
    }

    @Test
    @DisplayName("listErrors: 按行号过滤异常行")
    void given_rowNoFilter_when_listErrors_then_filtered() {
        when(taskService.getTaskById(TASK_ID)).thenReturn(createImportTask());
        when(importErrorLogMapper.selectByTaskIdAndRowNo(TASK_ID, 42, 0, 20)).thenReturn(List.of(new ImportErrorLog()));
        when(importErrorLogMapper.countByTaskIdAndRowNo(TASK_ID, 42)).thenReturn(1L);

        PageResult<ImportErrorLog> result = importErrorLogService.listErrors(TASK_ID, 42, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("listErrors: 非导入任务抛异常")
    void given_exportTask_when_listErrors_then_throw() {
        Task task = createImportTask();
        task.setTaskType("EXPORT");
        when(taskService.getTaskById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> importErrorLogService.listErrors(TASK_ID, null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PARAM_ERROR.getCode());
    }
}
