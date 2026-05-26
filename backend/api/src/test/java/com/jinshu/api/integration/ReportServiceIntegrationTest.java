package com.jinshu.api.integration;

import com.jinshu.api.dao.ReportMapper;
import com.jinshu.api.dao.ReportWorkflowMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.api.service.ReportService;
import com.jinshu.common.entity.Report;
import com.jinshu.common.entity.ReportWorkflow;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("ReportService - DB integration tests")
class ReportServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ReportWorkflowMapper workflowMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected void createReviewerUser() {
        jdbcTemplate.update(
            "INSERT INTO users (id, tenant_id, username, password_hash, role, status, login_fail_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 0, NOW(), NOW())",
            2L, TENANT_ID, "reviewer", passwordEncoder.encode("Test@1234"), "USER", "ACTIVE"
        );
    }

    protected ReportService.SubmitReviewRequest createSubmitRequest() {
        var req = new ReportService.SubmitReviewRequest();
        req.setReviewerId(2L);
        return req;
    }

    @Nested
    @DisplayName("Create report")
    class Create {

        @Test
        @DisplayName("Should create report with DRAFT status and initial workflow record")
        void shouldCreateReport() {
            var request = new ReportService.CreateReportRequest();
            request.setName("测试报表");
            request.setDescription("描述");

            Report report = reportService.createReport(request);

            assertThat(report.getId()).isNotNull();
            assertThat(report.getName()).isEqualTo("测试报表");
            assertThat(report.getStatus()).isEqualTo("DRAFT");
            assertThat(report.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(report.getSchemaVersion()).isEqualTo(1);
            assertThat(report.getIsDeleted()).isFalse();

            List<ReportWorkflow> workflows = workflowMapper.selectByReportId(report.getId());
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0).getOperation()).isEqualTo("CREATE");
        }
    }

    @Nested
    @DisplayName("Get report")
    class Get {

        private Report saved;

        @BeforeEach
        void setUp() {
            var request = new ReportService.CreateReportRequest();
            request.setName("报表");
            saved = reportService.createReport(request);
        }

        @Test
        @DisplayName("Should return report by id")
        void shouldGetById() {
            Report report = reportService.getReportById(saved.getId());
            assertThat(report.getName()).isEqualTo("报表");
        }

        @Test
        @DisplayName("Should throw when report not found")
        void shouldThrowWhenNotFound() {
            assertThatThrownBy(() -> reportService.getReportById(99999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(2001));
        }

        @Test
        @DisplayName("Should throw when report is soft-deleted")
        void shouldThrowWhenDeleted() {
            reportService.deleteReport(saved.getId());
            assertThatThrownBy(() -> reportService.getReportById(saved.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(2001));
        }
    }

    @Nested
    @DisplayName("List reports")
    class ListReports {

        @BeforeEach
        void setUp() {
            for (int i = 0; i < 5; i++) {
                var req = new ReportService.CreateReportRequest();
                req.setName("报表" + i);
                reportService.createReport(req);
            }
        }

        @Test
        @DisplayName("Should return paginated results")
        void shouldListPaginated() {
            PageResult<Report> page1 = reportService.listReports(null, null, 1, 2);
            assertThat(page1.getList()).hasSize(2);
            assertThat(page1.getTotal()).isEqualTo(5);
            assertThat(page1.getPageNum()).isEqualTo(1);
            assertThat(page1.getPageSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should filter by name with LIKE")
        void shouldFilterByName() {
            PageResult<Report> result = reportService.listReports("报表1", null, 1, 10);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getName()).isEqualTo("报表1");
        }
    }

    @Nested
    @DisplayName("Update report")
    class Update {

        private Report saved;

        @BeforeEach
        void setUp() {
            createReviewerUser();
            var req = new ReportService.CreateReportRequest();
            req.setName("原始名称");
            saved = reportService.createReport(req);
        }

        @Test
        @DisplayName("Should update fields and bump schema version")
        void shouldUpdate() {
            var updateReq = new ReportService.UpdateReportRequest();
            updateReq.setName("新名称");
            updateReq.setDescription("新描述");

            Report updated = reportService.updateReport(saved.getId(), updateReq);

            assertThat(updated.getName()).isEqualTo("新名称");
            assertThat(updated.getDescription()).isEqualTo("新描述");
            assertThat(updated.getSchemaVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw when status is not DRAFT or REJECTED")
        void shouldThrowWhenWrongStatus() {
            reportService.submitForReview(saved.getId(), createSubmitRequest());

            var updateReq = new ReportService.UpdateReportRequest();
            updateReq.setName("无法更新");
            assertThatThrownBy(() -> reportService.updateReport(saved.getId(), updateReq))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Status workflow")
    class Workflow {

        private Report report;

        @BeforeEach
        void setUp() {
            createReviewerUser();
            var req = new ReportService.CreateReportRequest();
            req.setName("工作流测试");
            report = reportService.createReport(req);
        }

        @Test
        @DisplayName("DRAFT -> PENDING_REVIEW")
        void shouldSubmitForReview() {
            Report submitted = reportService.submitForReview(report.getId(), createSubmitRequest());
            assertThat(submitted.getStatus()).isEqualTo("PENDING_REVIEW");
        }

        @Test
        @DisplayName("PENDING_REVIEW -> APPROVED")
        void shouldApprove() {
            reportService.submitForReview(report.getId(), createSubmitRequest());

            var reviewReq = new ReportService.ReviewRequest();
            reviewReq.setComment("通过");
            Report approved = reportService.approveReport(report.getId(), reviewReq);
            assertThat(approved.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("PENDING_REVIEW -> REJECTED")
        void shouldReject() {
            reportService.submitForReview(report.getId(), createSubmitRequest());

            var reviewReq = new ReportService.ReviewRequest();
            reviewReq.setComment("驳回");
            Report rejected = reportService.rejectReport(report.getId(), reviewReq);
            assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("APPROVED -> PUBLISHED")
        void shouldPublish() {
            reportService.submitForReview(report.getId(), createSubmitRequest());
            reportService.approveReport(report.getId(), new ReportService.ReviewRequest());

            Report published = reportService.publishReport(report.getId());
            assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("Cannot approve when REJECTED")
        void shouldNotApproveWhenRejected() {
            reportService.submitForReview(report.getId(), createSubmitRequest());
            reportService.rejectReport(report.getId(), new ReportService.ReviewRequest());

            assertThatThrownBy(() -> reportService.approveReport(report.getId(), new ReportService.ReviewRequest()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Cannot approve directly from DRAFT")
        void shouldNotApproveWhenDraft() {
            assertThatThrownBy(() -> reportService.approveReport(report.getId(), new ReportService.ReviewRequest()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("REJECTED -> PENDING_REVIEW (resubmit)")
        void shouldResubmitAfterRejection() {
            reportService.submitForReview(report.getId(), createSubmitRequest());
            reportService.rejectReport(report.getId(), new ReportService.ReviewRequest());

            Report resubmitted = reportService.submitForReview(report.getId(), createSubmitRequest());
            assertThat(resubmitted.getStatus()).isEqualTo("PENDING_REVIEW");
        }
    }

    @Nested
    @DisplayName("Delete report")
    class Delete {

        @Test
        @DisplayName("Should soft-delete report")
        void shouldSoftDelete() {
            var req = new ReportService.CreateReportRequest();
            req.setName("待删除");
            Report saved = reportService.createReport(req);

            reportService.deleteReport(saved.getId());

            Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM report_metadata WHERE id = ?", Boolean.class, saved.getId());
            assertThat(isDeleted).isTrue();
        }
    }

}
