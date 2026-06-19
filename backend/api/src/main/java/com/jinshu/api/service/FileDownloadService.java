package com.jinshu.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.TaskMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.entity.Task;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.utils.FileNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 文件下载服务
 *
 * 负责根据任务 ID 解析实际可下载文件的路径、大小与名称，
 * 并进行租户隔离与路径安全校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    /**
     * 可下载文件信息
     */
    public record DownloadFile(Path path, long size, String fileName) {
    }

    /**
     * 根据任务 ID 解析可下载文件信息
     *
     * @param taskId 任务 ID
     * @return 下载文件信息
     */
    public DownloadFile resolveDownloadFile(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }

        Long tenantId = TenantContext.getTenantId();
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ERROR, "任务未完成，无法下载");
        }

        String filePath = resolveFilePath(task);
        Path path = Path.of(filePath).toAbsolutePath().normalize();

        validatePathUnderBasePath(path);

        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法文件路径");
        }

        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            log.error("无法读取文件大小: {}", path, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法读取文件大小");
        }

        String fileName = task.getResultFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = path.getFileName().toString();
        }

        return new DownloadFile(path, size, FileNameUtil.getSafeFileName(fileName));
    }

    private String resolveFilePath(Task task) {
        if (task.getResultFilePath() != null && !task.getResultFilePath().isBlank()) {
            return task.getResultFilePath();
        }

        // 兼容旧任务：从 parameters JSON 中解析 outputPath
        if (task.getParameters() != null && !task.getParameters().isBlank()) {
            try {
                Map<String, Object> params = objectMapper.readValue(task.getParameters(), new TypeReference<>() {});
                String outputPath = (String) params.get("outputPath");
                if (outputPath != null && !outputPath.isBlank()) {
                    return outputPath;
                }
            } catch (Exception e) {
                log.warn("解析任务参数失败, taskId={}", task.getId(), e);
            }
        }

        throw new BusinessException(ErrorCode.PARAM_ERROR, "文件路径不存在");
    }

    private void validatePathUnderBasePath(Path path) {
        Path basePath = Path.of(FileNameUtil.getBasePath()).toAbsolutePath().normalize();
        if (!path.startsWith(basePath)) {
            log.warn("非法文件下载路径: {}, basePath={}", path, basePath);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法文件路径");
        }
    }
}
