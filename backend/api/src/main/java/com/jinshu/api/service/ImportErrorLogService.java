package com.jinshu.api.service;

import com.jinshu.common.dao.ImportErrorLogMapper;
import com.jinshu.common.entity.ImportErrorLog;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportErrorLogService {

    private final ImportErrorLogMapper importErrorLogMapper;
    private final TaskService taskService;

    public PageResult<ImportErrorLog> listErrors(Long taskId, Integer rowNo, int page, int pageSize) {
        Task task = taskService.getTaskById(taskId);
        if (!"IMPORT".equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持导入任务的异常行查询");
        }

        int offset = (page - 1) * pageSize;
        List<ImportErrorLog> list;
        long total;
        if (rowNo == null) {
            list = importErrorLogMapper.selectByTaskId(taskId, offset, pageSize);
            total = importErrorLogMapper.countByTaskId(taskId);
        } else {
            list = importErrorLogMapper.selectByTaskIdAndRowNo(taskId, rowNo, offset, pageSize);
            total = importErrorLogMapper.countByTaskIdAndRowNo(taskId, rowNo);
        }
        return PageResult.of(list, total, page, pageSize);
    }
}
