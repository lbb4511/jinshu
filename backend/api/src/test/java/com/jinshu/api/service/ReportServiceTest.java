package com.jinshu.api.service;

import com.jinshu.api.dao.ReportMapper;
import com.jinshu.api.dao.ReportWorkflowMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.ReportWorkflow;
import com.jinshu.common.entity.User;
import com.jinshu.common.enums.ReportStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportService 报表生命周期服务测试")
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReportWorkflowMapper workflowMapper;
    @Mock
    private UserService userService;

    private ReportService reportService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;
    private static final Long REPORT_ID = 200L;
    private static final Long REVIEWER_ID = 99L;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportMapper, workflowMapper, userService);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername("testuser");
        UserContext.setRole("USER");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    private Report createDraftReport() {
        Report report = new Report();
        report.setId(REPORT_ID);
        report.setTenantId(TENANT_ID);
        report.setName("测试报表");
        report.setStatus(ReportStatus.DRAFT.name());
        report.setCreatedBy(USER_ID);
        report.setSchemaVersion(1);
        report.setIsDeleted(false);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return report;
    }

    private User createReviewer() {
        User reviewer = new User();
        reviewer.setId(REVIEWER_ID);
        reviewer.setTenantId(TENANT_ID);
        reviewer.setUsername("reviewer");
        reviewer.setStatus("ACTIVE");
        reviewer.setRole("ADMIN");
        return reviewer;
    }

    // ============ createReport ============

    @Test
    @DisplayName("createReport：创建草稿成功，初始状态 DRAFT")
    void given_validRequest_when_createReport_then_draftCreated() {
        when(reportMapper.insert(any(Report.class))).thenAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(REPORT_ID);
            return 1;
        });

        ReportService.CreateReportRequest request = new ReportService.CreateReportRequest();
        request.setName("销售报表");
        request.setDescription("季度销售数据");

        Report result = reportService.createReport(request);

        assertThat(result.getName()).isEqualTo("销售报表");
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(result.getSchemaVersion()).isEqualTo(1);
        assertThat(result.getIsDeleted()).isFalse();
        verify(workflowMapper).insert(argThat(w -> "DRAFT".equals(w.getStatus())));
    }

    // ============ submitForReview ============

    @Test
    @DisplayName("submitForReview：DRAFT → PENDING_REVIEW，创建 workflow 记录")
    void given_draftReport_when_submitForReview_then_pendingReview() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());
        when(userService.getUserById(REVIEWER_ID)).thenReturn(createReviewer());

        ReportService.SubmitReviewRequest request = new ReportService.SubmitReviewRequest();
        request.setReviewerId(REVIEWER_ID);

        Report result = reportService.submitForReview(REPORT_ID, request);

        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        verify(reportMapper).update(argThat(r -> "PENDING_REVIEW".equals(r.getStatus())));
        verify(workflowMapper).insert(argThat(w ->
                "PENDING_REVIEW".equals(w.getStatus()) &&
                        REVIEWER_ID.equals(w.getReviewedBy()) &&
                        "SUBMIT".equals(w.getOperation())));
    }

    @Test
    @DisplayName("submitForReview：REJECTED → PENDING_REVIEW 允许重新提交")
    void given_rejectedReport_when_submitForReview_then_pendingReview() {
        Report rejected = createDraftReport();
        rejected.setStatus(ReportStatus.REJECTED.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(rejected);
        when(userService.getUserById(REVIEWER_ID)).thenReturn(createReviewer());

        ReportService.SubmitReviewRequest request = new ReportService.SubmitReviewRequest();
        request.setReviewerId(REVIEWER_ID);

        Report result = reportService.submitForReview(REPORT_ID, request);

        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("submitForReview：非 DRAFT/REJECTED 状态不允许提交")
    void given_approvedReport_when_submitForReview_then_throw() {
        Report approved = createDraftReport();
        approved.setStatus(ReportStatus.APPROVED.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(approved);

        ReportService.SubmitReviewRequest request = new ReportService.SubmitReviewRequest();
        request.setReviewerId(REVIEWER_ID);

        assertThatThrownBy(() -> reportService.submitForReview(REPORT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不允许提交审查");
    }

    @Test
    @DisplayName("submitForReview：不能指定自己为审查人")
    void given_selfAsReviewer_when_submitForReview_then_throw() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());
        User selfReviewer = createReviewer();
        selfReviewer.setId(USER_ID);
        when(userService.getUserById(USER_ID)).thenReturn(selfReviewer);

        ReportService.SubmitReviewRequest request = new ReportService.SubmitReviewRequest();
        request.setReviewerId(USER_ID);

        assertThatThrownBy(() -> reportService.submitForReview(REPORT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能指定自己为审查人");
    }

    @Test
    @DisplayName("submitForReview：审查人已禁用抛异常")
    void given_disabledReviewer_when_submitForReview_then_throw() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());
        User disabled = createReviewer();
        disabled.setStatus("DISABLED");
        when(userService.getUserById(REVIEWER_ID)).thenReturn(disabled);

        ReportService.SubmitReviewRequest request = new ReportService.SubmitReviewRequest();
        request.setReviewerId(REVIEWER_ID);

        assertThatThrownBy(() -> reportService.submitForReview(REPORT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审查人状态无效");
    }

    // ============ approveReport ============

    @Test
    @DisplayName("approveReport：PENDING_REVIEW → APPROVED")
    void given_pendingReview_when_approve_then_approved() {
        Report pending = createDraftReport();
        pending.setStatus(ReportStatus.PENDING_REVIEW.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pending);
        when(workflowMapper.selectCurrentByReportId(REPORT_ID)).thenReturn(null);

        ReportService.ReviewRequest request = new ReportService.ReviewRequest();
        request.setComment("数据准确");

        Report result = reportService.approveReport(REPORT_ID, request);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(workflowMapper).insert(argThat(w ->
                "APPROVED".equals(w.getStatus()) && "APPROVE".equals(w.getOperation())));
    }

    @Test
    @DisplayName("approveReport：非 PENDING_REVIEW 状态抛异常")
    void given_draftReport_when_approve_then_throw() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());

        ReportService.ReviewRequest request = new ReportService.ReviewRequest();
        assertThatThrownBy(() -> reportService.approveReport(REPORT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不允许审批");
    }

    @Test
    @DisplayName("approveReport：非审查人且非 ADMIN 无权审批")
    void given_nonReviewerNonAdmin_when_approve_then_throw() {
        Report pending = createDraftReport();
        pending.setStatus(ReportStatus.PENDING_REVIEW.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pending);
        ReportWorkflow currentWorkflow = new ReportWorkflow();
        currentWorkflow.setReviewedBy(REVIEWER_ID);
        when(workflowMapper.selectCurrentByReportId(REPORT_ID)).thenReturn(currentWorkflow);

        UserContext.setRole("USER");

        ReportService.ReviewRequest request = new ReportService.ReviewRequest();
        assertThatThrownBy(() -> reportService.approveReport(REPORT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权审批");
    }

    // ============ rejectReport ============

    @Test
    @DisplayName("rejectReport：PENDING_REVIEW → REJECTED")
    void given_pendingReview_when_reject_then_rejected() {
        Report pending = createDraftReport();
        pending.setStatus(ReportStatus.PENDING_REVIEW.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(pending);
        when(workflowMapper.selectCurrentByReportId(REPORT_ID)).thenReturn(null);

        ReportService.ReviewRequest request = new ReportService.ReviewRequest();
        request.setComment("数据有误");

        Report result = reportService.rejectReport(REPORT_ID, request);

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(workflowMapper).insert(argThat(w ->
                "REJECTED".equals(w.getStatus()) && "REJECT".equals(w.getOperation())));
    }

    // ============ publishReport ============

    @Test
    @DisplayName("publishReport：APPROVED → PUBLISHED")
    void given_approvedReport_when_publish_then_published() {
        Report approved = createDraftReport();
        approved.setStatus(ReportStatus.APPROVED.name());
        when(reportMapper.selectById(REPORT_ID)).thenReturn(approved);

        Report result = reportService.publishReport(REPORT_ID);

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        verify(workflowMapper).insert(argThat(w ->
                "PUBLISHED".equals(w.getStatus()) && "PUBLISH".equals(w.getOperation())));
    }

    @Test
    @DisplayName("publishReport：非 APPROVED 状态抛异常")
    void given_draftReport_when_publish_then_throw() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());

        assertThatThrownBy(() -> reportService.publishReport(REPORT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不允许发布");
    }

    // ============ softDelete ============

    @Test
    @DisplayName("deleteReport：创建人删除自己报表成功")
    void given_owner_when_delete_then_softDelete() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());

        reportService.deleteReport(REPORT_ID);

        verify(reportMapper).update(argThat(Report::getIsDeleted));
    }

    @Test
    @DisplayName("deleteReport：非创建人且非 ADMIN 抛异常")
    void given_nonOwnerNonAdmin_when_delete_then_throw() {
        Report report = createDraftReport();
        report.setCreatedBy(999L);
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report);
        UserContext.setRole("USER");

        assertThatThrownBy(() -> reportService.deleteReport(REPORT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权删除");
    }

    @Test
    @DisplayName("deleteReport：ADMIN 可删除他人报表")
    void given_admin_when_deleteOtherReport_then_success() {
        Report report = createDraftReport();
        report.setCreatedBy(999L);
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report);
        UserContext.setRole("ADMIN");

        reportService.deleteReport(REPORT_ID);

        verify(reportMapper).update(argThat(Report::getIsDeleted));
    }

    // ============ listReports ============

    @Test
    @DisplayName("listReports：分页查询成功")
    void given_pagination_when_listReports_then_returnPage() {
        when(reportMapper.selectList(eq(TENANT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createDraftReport()));
        when(reportMapper.countList(eq(TENANT_ID), any(), any())).thenReturn(1L);

        PageResult<Report> result = reportService.listReports(null, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    // ============ getReportById ============

    @Test
    @DisplayName("getReportById：存在返回正确")
    void given_existingReport_when_getById_then_return() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(createDraftReport());

        Report result = reportService.getReportById(REPORT_ID);

        assertThat(result.getId()).isEqualTo(REPORT_ID);
        assertThat(result.getName()).isEqualTo("测试报表");
    }

    @Test
    @DisplayName("getReportById：不存在抛异常")
    void given_nonExistent_when_getById_then_throw() {
        when(reportMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> reportService.getReportById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REPORT_NOT_FOUND.getCode());
    }
}
