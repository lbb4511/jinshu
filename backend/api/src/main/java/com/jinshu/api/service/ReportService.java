package com.jinshu.api.service;

import com.jinshu.api.dao.ReportMapper;
import com.jinshu.api.dao.ReportWorkflowMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.ReportWorkflow;
import com.jinshu.common.entity.User;
import com.jinshu.common.enums.ReportStatus;
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
public class ReportService {

    private final ReportMapper reportMapper;
    private final ReportWorkflowMapper workflowMapper;
    private final UserService userService;

    @Transactional
    @AuditLog(operation = "CREATE_REPORT", targetType = "REPORT")
    public Report createReport(CreateReportRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        Report report = new Report();
        report.setTenantId(tenantId);
        report.setName(request.getName());
        report.setDescription(request.getDescription());
        report.setDataSourceId(request.getDataSourceId());
        report.setTemplateConfig(request.getTemplateConfig());
        report.setCreatedBy(userId);
        report.setSchemaVersion(1);
        report.setStatus(ReportStatus.DRAFT.name());
        report.setIsDeleted(false);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        reportMapper.insert(report);

        ReportWorkflow workflow = new ReportWorkflow();
        workflow.setReportId(report.getId());
        workflow.setTenantId(tenantId);
        workflow.setStatus(ReportStatus.DRAFT.name());
        workflow.setOperatorId(userId);
        workflow.setOperation("CREATE");
        workflow.setCreatedAt(LocalDateTime.now());
        workflowMapper.insert(workflow);

        return report;
    }

    public Report getReportById(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null || Boolean.TRUE.equals(report.getIsDeleted())) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }

    public PageResult<Report> listReports(String name, String status, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;
        List<Report> reports = reportMapper.selectList(tenantId, name, status, offset, pageSize);
        long total = reportMapper.countList(tenantId, name, status);
        return PageResult.of(reports, total, page, pageSize);
    }

    @Transactional
    @AuditLog(operation = "UPDATE_REPORT", targetType = "REPORT")
    public Report updateReport(Long id, UpdateReportRequest request) {
        Report report = getReportById(id);

        PermissionUtils.checkOwner(report.getCreatedBy());

        if (!ReportStatus.DRAFT.name().equals(report.getStatus()) &&
            !ReportStatus.REJECTED.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许编辑");
        }

        if (request.getName() != null) {
            report.setName(request.getName());
        }
        if (request.getDescription() != null) {
            report.setDescription(request.getDescription());
        }
        if (request.getDataSourceId() != null) {
            report.setDataSourceId(request.getDataSourceId());
        }
        if (request.getTemplateConfig() != null) {
            report.setTemplateConfig(request.getTemplateConfig());
        }

        report.setSchemaVersion(report.getSchemaVersion() + 1);
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);

        return report;
    }

    @Transactional
    @AuditLog(operation = "DELETE_REPORT", targetType = "REPORT")
    public void deleteReport(Long id) {
        Report report = getReportById(id);

        PermissionUtils.checkOwner(report.getCreatedBy());

        report.setIsDeleted(true);
        report.setDeletedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);
    }

    @Transactional
    @AuditLog(operation = "SUBMIT_REVIEW", targetType = "REPORT")
    public Report submitForReview(Long id, SubmitReviewRequest request) {
        Report report = getReportById(id);
        Long userId = UserContext.getUserId();

        if (!ReportStatus.DRAFT.name().equals(report.getStatus()) &&
            !ReportStatus.REJECTED.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许提交审查");
        }

        User reviewer = userService.getUserById(request.getReviewerId());
        if (reviewer.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能指定自己为审查人");
        }

        if (!"ACTIVE".equals(reviewer.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审查人状态无效");
        }

        report.setStatus(ReportStatus.PENDING_REVIEW.name());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);

        ReportWorkflow workflow = new ReportWorkflow();
        workflow.setReportId(id);
        workflow.setTenantId(report.getTenantId());
        workflow.setStatus(ReportStatus.PENDING_REVIEW.name());
        workflow.setReviewedBy(request.getReviewerId());
        workflow.setOperatorId(userId);
        workflow.setOperation("SUBMIT");
        workflow.setCreatedAt(LocalDateTime.now());
        workflowMapper.insert(workflow);

        return report;
    }

    @Transactional
    @AuditLog(operation = "APPROVE_REPORT", targetType = "REPORT")
    public Report approveReport(Long id, ReviewRequest request) {
        Report report = getReportById(id);
        Long userId = UserContext.getUserId();

        if (!ReportStatus.PENDING_REVIEW.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批");
        }

        ReportWorkflow currentWorkflow = workflowMapper.selectCurrentByReportId(id);
        if (currentWorkflow != null && currentWorkflow.getReviewedBy() != null) {
            PermissionUtils.checkReviewer(currentWorkflow.getReviewedBy());
        }

        report.setStatus(ReportStatus.APPROVED.name());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);

        ReportWorkflow workflow = new ReportWorkflow();
        workflow.setReportId(id);
        workflow.setTenantId(report.getTenantId());
        workflow.setStatus(ReportStatus.APPROVED.name());
        workflow.setReviewedBy(userId);
        workflow.setReviewComment(request.getComment());
        workflow.setReviewedAt(LocalDateTime.now());
        workflow.setOperatorId(userId);
        workflow.setOperation("APPROVE");
        workflow.setCreatedAt(LocalDateTime.now());
        workflowMapper.insert(workflow);

        return report;
    }

    @Transactional
    @AuditLog(operation = "REJECT_REPORT", targetType = "REPORT")
    public Report rejectReport(Long id, ReviewRequest request) {
        Report report = getReportById(id);
        Long userId = UserContext.getUserId();

        if (!ReportStatus.PENDING_REVIEW.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批");
        }

        ReportWorkflow currentWorkflow = workflowMapper.selectCurrentByReportId(id);
        if (currentWorkflow != null && currentWorkflow.getReviewedBy() != null) {
            PermissionUtils.checkReviewer(currentWorkflow.getReviewedBy());
        }

        report.setStatus(ReportStatus.REJECTED.name());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);

        ReportWorkflow workflow = new ReportWorkflow();
        workflow.setReportId(id);
        workflow.setTenantId(report.getTenantId());
        workflow.setStatus(ReportStatus.REJECTED.name());
        workflow.setReviewedBy(userId);
        workflow.setReviewComment(request.getComment());
        workflow.setReviewedAt(LocalDateTime.now());
        workflow.setOperatorId(userId);
        workflow.setOperation("REJECT");
        workflow.setCreatedAt(LocalDateTime.now());
        workflowMapper.insert(workflow);

        return report;
    }

    @Transactional
    @AuditLog(operation = "PUBLISH_REPORT", targetType = "REPORT")
    public Report publishReport(Long id) {
        Report report = getReportById(id);
        Long userId = UserContext.getUserId();

        if (!ReportStatus.APPROVED.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许发布");
        }

        report.setStatus(ReportStatus.PUBLISHED.name());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.update(report);

        ReportWorkflow workflow = new ReportWorkflow();
        workflow.setReportId(id);
        workflow.setTenantId(report.getTenantId());
        workflow.setStatus(ReportStatus.PUBLISHED.name());
        workflow.setOperatorId(userId);
        workflow.setOperation("PUBLISH");
        workflow.setCreatedAt(LocalDateTime.now());
        workflowMapper.insert(workflow);

        return report;
    }

    public static class CreateReportRequest {
        private String name;
        private String description;
        private Long dataSourceId;
        private String templateConfig;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getTemplateConfig() { return templateConfig; }
        public void setTemplateConfig(String templateConfig) { this.templateConfig = templateConfig; }
    }

    public static class UpdateReportRequest {
        private String name;
        private String description;
        private Long dataSourceId;
        private String templateConfig;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getTemplateConfig() { return templateConfig; }
        public void setTemplateConfig(String templateConfig) { this.templateConfig = templateConfig; }
    }

    public static class SubmitReviewRequest {
        private Long reviewerId;

        public Long getReviewerId() { return reviewerId; }
        public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    }

    public static class ReviewRequest {
        private String comment;

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
