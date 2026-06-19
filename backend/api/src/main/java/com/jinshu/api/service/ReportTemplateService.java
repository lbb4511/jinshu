package com.jinshu.api.service;

import com.jinshu.api.dao.ReportTemplateMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.ReportTemplate;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private final ReportTemplateMapper reportTemplateMapper;
    private final ReportService reportService;

    @Transactional
    public ReportTemplate createTemplate(CreateTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        ReportTemplate template = new ReportTemplate();
        template.setTenantId(tenantId);
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setLayoutJson(request.getLayoutJson());
        template.setSampleData(request.getSampleData());
        template.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        template.setIsSystem(false);
        template.setStatus("ACTIVE");
        template.setCreatedBy(userId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        reportTemplateMapper.insert(template);
        return template;
    }

    @Transactional
    public ReportTemplate updateTemplate(Long id, UpdateTemplateRequest request) {
        ReportTemplate template = getAndCheckWritableTemplate(id);

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        if (request.getThumbnailUrl() != null) {
            template.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getLayoutJson() != null) {
            template.setLayoutJson(request.getLayoutJson());
        }
        if (request.getSampleData() != null) {
            template.setSampleData(request.getSampleData());
        }
        if (request.getIsPublic() != null) {
            template.setIsPublic(request.getIsPublic());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }

        template.setUpdatedAt(LocalDateTime.now());
        reportTemplateMapper.update(template);
        return template;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ReportTemplate template = getAndCheckWritableTemplate(id);
        reportTemplateMapper.deleteById(template.getId());
    }

    public ReportTemplate getTemplate(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null || !isVisible(template)) {
            throw new BusinessException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    public PageResult<ReportTemplate> listTemplates(String category, String keyword,
                                                    Boolean includePublic, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;

        List<ReportTemplate> list;
        long total;

        if (Boolean.TRUE.equals(includePublic)) {
            list = reportTemplateMapper.selectPublicTemplates(tenantId, category, "ACTIVE", keyword, offset, pageSize);
            total = reportTemplateMapper.countPublicTemplates(tenantId, category, "ACTIVE", keyword);
        } else {
            list = reportTemplateMapper.selectList(tenantId, category, "ACTIVE", keyword, offset, pageSize);
            total = reportTemplateMapper.countList(tenantId, category, "ACTIVE", keyword);
        }

        return PageResult.of(list, total, page, pageSize);
    }

    @Transactional
    public Long applyTemplate(Long id, ApplyTemplateRequest request) {
        ReportTemplate template = getTemplate(id);

        ReportService.CreateReportRequest createRequest = new ReportService.CreateReportRequest();
        createRequest.setName(request.getName() != null ? request.getName() : template.getName());
        createRequest.setDescription(request.getDescription() != null ? request.getDescription() : template.getDescription());
        createRequest.setDataSourceId(request.getDataSourceId());
        createRequest.setTemplateConfig(template.getLayoutJson());

        Report report = reportService.createReport(createRequest);
        return report.getId();
    }

    private ReportTemplate getAndCheckWritableTemplate(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.REPORT_TEMPLATE_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统级模板不可修改或删除");
        }

        Long tenantId = TenantContext.getTenantId();
        if (!Long.valueOf(0L).equals(template.getTenantId()) && !template.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.CROSS_TENANT_DENIED);
        }

        PermissionUtils.checkOwner(template.getCreatedBy());
        return template;
    }

    private boolean isVisible(ReportTemplate template) {
        Long tenantId = TenantContext.getTenantId();
        if (Long.valueOf(0L).equals(template.getTenantId())) {
            return true;
        }
        if (template.getTenantId().equals(tenantId)) {
            return true;
        }
        return Boolean.TRUE.equals(template.getIsPublic()) && "ACTIVE".equals(template.getStatus());
    }

    public static class CreateTemplateRequest {
        private String name;
        private String description;
        private String category;
        private String thumbnailUrl;
        private String layoutJson;
        private String sampleData;
        private Boolean isPublic;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getLayoutJson() { return layoutJson; }
        public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
        public String getSampleData() { return sampleData; }
        public void setSampleData(String sampleData) { this.sampleData = sampleData; }
        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    }

    public static class UpdateTemplateRequest {
        private String name;
        private String description;
        private String category;
        private String thumbnailUrl;
        private String layoutJson;
        private String sampleData;
        private Boolean isPublic;
        private String status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getLayoutJson() { return layoutJson; }
        public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
        public String getSampleData() { return sampleData; }
        public void setSampleData(String sampleData) { this.sampleData = sampleData; }
        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ApplyTemplateRequest {
        private String name;
        private String description;
        private Long dataSourceId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    }
}
