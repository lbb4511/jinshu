package com.jinshu.api.service;

import com.jinshu.api.dao.ExcelTemplateFileMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.ExcelTemplateFile;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.PermissionUtils;
import com.jinshu.common.utils.FileNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Excel 模板文件服务
 *
 * 负责模板文件的上传、查询与删除，文件按租户隔离存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateFileService {

    private static final String TEMPLATE_SUB_DIR = "templates";
    private static final String EXCEL_EXTENSION = ".xlsx";

    private final ExcelTemplateFileMapper excelTemplateFileMapper;

    @Transactional
    public ExcelTemplateFile uploadTemplate(Long reportId, MultipartFile file) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        validateExcelFile(file);

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        String storedName = UUID.randomUUID() + ext;
        Path targetPath = resolveTemplatePath(tenantId, storedName);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("Failed to save template file for tenant={}", tenantId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板文件保存失败");
        }

        ExcelTemplateFile record = new ExcelTemplateFile();
        record.setTenantId(tenantId);
        record.setReportId(reportId);
        record.setFileName(originalName);
        record.setFilePath(targetPath.toString());
        record.setFileSize(file.getSize());
        record.setCreatedBy(userId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        excelTemplateFileMapper.insert(record);
        log.info("Template uploaded: id={}, tenantId={}, reportId={}, path={}",
                record.getId(), tenantId, reportId, record.getFilePath());
        return record;
    }

    public PageResult<ExcelTemplateFile> listTemplates(Long reportId, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;
        var list = excelTemplateFileMapper.selectList(tenantId, reportId, offset, pageSize);
        long total = excelTemplateFileMapper.countList(tenantId, reportId);
        return PageResult.of(list, total, page, pageSize);
    }

    public ExcelTemplateFile getTemplate(Long id) {
        Long tenantId = TenantContext.getTenantId();
        ExcelTemplateFile record = excelTemplateFileMapper.selectById(id);
        if (record == null || !record.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板文件不存在");
        }
        return record;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ExcelTemplateFile record = getTemplate(id);
        PermissionUtils.checkOwner(record.getCreatedBy());

        try {
            Path path = Path.of(record.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete template file: {}", record.getFilePath(), e);
        }

        excelTemplateFileMapper.deleteById(id);
        log.info("Template deleted: id={}, tenantId={}", id, record.getTenantId());
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(EXCEL_EXTENSION)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "仅支持 .xlsx 模板文件");
        }
    }

    private Path resolveTemplatePath(Long tenantId, String storedName) {
        String basePath = FileNameUtil.getBasePath();
        return Path.of(basePath, TEMPLATE_SUB_DIR, String.valueOf(tenantId), storedName).toAbsolutePath().normalize();
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return EXCEL_EXTENSION;
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : EXCEL_EXTENSION;
    }
}
