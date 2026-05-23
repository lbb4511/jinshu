package com.jinshu.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportStatus 报表状态枚举测试")
class ReportStatusTest {

    @Test
    @DisplayName("枚举元素完整：6 个状态")
    void given_enum_when_values_then_allStatuses() {
        assertThat(ReportStatus.values())
                .containsExactly(
                        ReportStatus.DRAFT,
                        ReportStatus.PENDING_REVIEW,
                        ReportStatus.APPROVED,
                        ReportStatus.REJECTED,
                        ReportStatus.PUBLISHED
                );
    }

    @Test
    @DisplayName("Status 数量：5 个")
    void given_enum_when_count_then_5() {
        assertThat(ReportStatus.values()).hasSize(5);
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, 草稿",
            "PENDING_REVIEW, 待审查",
            "APPROVED, 已通过",
            "REJECTED, 已驳回",
            "PUBLISHED, 已公开"
    })
    @DisplayName("标签映射正确")
    void given_status_when_getLabel_then_match(String name, String expectedLabel) {
        ReportStatus status = ReportStatus.valueOf(name);
        assertThat(status.getLabel()).isEqualTo(expectedLabel);
    }

    @Test
    @DisplayName("有效状态跳转：DRAFT → PENDING_REVIEW")
    void given_draft_when_transitionToPendingReview_then_valid() {
        assertThat(ReportStatus.isValidTransition("DRAFT", "PENDING_REVIEW")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：DRAFT → DELETED（软删除）")
    void given_draft_when_transitionToDeleted_then_valid() {
        assertThat(ReportStatus.isValidTransition("DRAFT", "DELETED")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：PENDING_REVIEW → APPROVED")
    void given_pendingReview_when_transitionToApproved_then_valid() {
        assertThat(ReportStatus.isValidTransition("PENDING_REVIEW", "APPROVED")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：PENDING_REVIEW → REJECTED")
    void given_pendingReview_when_transitionToRejected_then_valid() {
        assertThat(ReportStatus.isValidTransition("PENDING_REVIEW", "REJECTED")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：REJECTED → DRAFT")
    void given_rejected_when_transitionToDraft_then_valid() {
        assertThat(ReportStatus.isValidTransition("REJECTED", "DRAFT")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：REJECTED → PENDING_REVIEW")
    void given_rejected_when_transitionToPendingReview_then_valid() {
        assertThat(ReportStatus.isValidTransition("REJECTED", "PENDING_REVIEW")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：APPROVED → PUBLISHED")
    void given_approved_when_transitionToPublished_then_valid() {
        assertThat(ReportStatus.isValidTransition("APPROVED", "PUBLISHED")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：APPROVED → REJECTED")
    void given_approved_when_transitionToRejected_then_valid() {
        assertThat(ReportStatus.isValidTransition("APPROVED", "REJECTED")).isTrue();
    }

    @Test
    @DisplayName("有效状态跳转：PUBLISHED → REJECTED")
    void given_published_when_transitionToRejected_then_valid() {
        assertThat(ReportStatus.isValidTransition("PUBLISHED", "REJECTED")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED, false",           // 草稿不能跳过审查直接通过
            "DRAFT, PUBLISHED, false",           // 草稿不能直接发布
            "PENDING_REVIEW, DRAFT, false",      // 审查中不可回退
            "PENDING_REVIEW, PUBLISHED, false",  // 审查中不可发布
            "APPROVED, DRAFT, false",            // 已通过不可回退草稿
            "APPROVED, PENDING_REVIEW, false",   // 已通过不可再审查
            "PUBLISHED, DRAFT, false",           // 已公开不可回退草稿
            "PUBLISHED, APPROVED, false",        // 已公开不可回退已通过
            "PUBLISHED, PENDING_REVIEW, false",  // 已公开不可回退审查中
            "null, APPROVED, false",             // null 只能到 DRAFT
            "UNKNOWN, DRAFT, false"              // 非法状态
    })
    @DisplayName("非法状态跳转全部被拒绝")
    void given_illegalTransitions_when_validate_then_false(String from, String to, boolean expected) {
        assertThat(ReportStatus.isValidTransition(from, to)).isFalse();
    }

    @Test
    @DisplayName("null from 允许转换到 DRAFT")
    void given_nullFrom_when_transitionToDraft_then_valid() {
        assertThat(ReportStatus.isValidTransition(null, "DRAFT")).isTrue();
    }

    @Test
    @DisplayName("null from 不允许转换到其他状态")
    void given_nullFrom_when_transitionToNonDraft_then_invalid() {
        assertThat(ReportStatus.isValidTransition(null, "PUBLISHED")).isFalse();
    }

    @Test
    @DisplayName("name() 输出可作为数据库存储值")
    void given_anyStatus_when_name_then_upperSnakeCase() {
        assertThat(ReportStatus.DRAFT.name()).isEqualTo("DRAFT");
        assertThat(ReportStatus.PENDING_REVIEW.name()).isEqualTo("PENDING_REVIEW");
        assertThat(ReportStatus.APPROVED.name()).isEqualTo("APPROVED");
        assertThat(ReportStatus.REJECTED.name()).isEqualTo("REJECTED");
        assertThat(ReportStatus.PUBLISHED.name()).isEqualTo("PUBLISHED");
    }
}
