package com.jinshu.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode 错误码枚举测试")
class ErrorCodeTest {

    @Test
    @DisplayName("所有枚举实例有 code 和 message")
    void given_allEnums_when_getCodeAndMessage_then_notNull() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getCode()).as("ErrorCode %s 缺少 code", ec.name()).isNotNull();
            assertThat(ec.getMessage()).as("ErrorCode %s 缺少 message", ec.name()).isNotBlank();
        }
    }

    @Test
    @DisplayName("码段规划：通用码段为 0-99")
    void given_success_when_getCode_then_0() {
        assertThat(ErrorCode.SUCCESS.getCode()).isZero();
    }

    @Test
    @DisplayName("HTTP 标准码段：400-499")
    void given_httpErrorCodes_when_getCode_then_inHttpRange() {
        assertThat(ErrorCode.PARAM_ERROR.getCode()).isEqualTo(400);
        assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(ErrorCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(ErrorCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(ErrorCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("租户/用户码段：1000-1999")
    void given_tenantUserCodes_when_getCode_then_in1000Range() {
        assertThat(ErrorCode.TENANT_NOT_FOUND.getCode()).isEqualTo(1001);
        assertThat(ErrorCode.USER_NOT_FOUND.getCode()).isEqualTo(1002);
        assertThat(ErrorCode.USERNAME_PASSWORD_ERROR.getCode()).isEqualTo(1003);
        assertThat(ErrorCode.ACCOUNT_LOCKED.getCode()).isEqualTo(1004);
        assertThat(ErrorCode.USER_DISABLED.getCode()).isEqualTo(1005);
        assertThat(ErrorCode.TENANT_DISABLED.getCode()).isEqualTo(1006);
    }

    @Test
    @DisplayName("报表码段：2000-2999")
    void given_reportCode_when_getCode_then_in2000Range() {
        assertThat(ErrorCode.REPORT_NOT_FOUND.getCode()).isEqualTo(2001);
    }

    @Test
    @DisplayName("任务码段：3000-3999")
    void given_taskCodes_when_getCode_then_in3000Range() {
        assertThat(ErrorCode.TASK_NOT_FOUND.getCode()).isEqualTo(3001);
        assertThat(ErrorCode.TASK_STATUS_ERROR.getCode()).isEqualTo(3002);
    }

    @Test
    @DisplayName("文件码段：4000-4999")
    void given_fileCodes_when_getCode_then_in4000Range() {
        assertThat(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode()).isEqualTo(4001);
        assertThat(ErrorCode.FILE_SIZE_EXCEEDED.getCode()).isEqualTo(4002);
    }

    @Test
    @DisplayName("数据源码段：5000-5999")
    void given_dataSourceCode_when_getCode_then_in5000Range() {
        assertThat(ErrorCode.DATA_SOURCE_CONNECT_FAILED.getCode()).isEqualTo(5001);
    }

    @Test
    @DisplayName("Token 错误码：401 与 UNAUTHORIZED 一致")
    void given_tokenErrorCodes_when_getCode_then_401() {
        assertThat(ErrorCode.TOKEN_EXPIRED.getCode()).isEqualTo(401);
        assertThat(ErrorCode.TOKEN_INVALID.getCode()).isEqualTo(401);
        assertThat(ErrorCode.REFRESH_TOKEN_INVALID.getCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("码段无重复（允许 HTTP 状态码映射段的重叠）")
    void given_allCodes_when_compare_then_noDuplicateInBusinessRange() {
        long distinctCount = java.util.Arrays.stream(ErrorCode.values())
                .filter(e -> e.getCode() < 400 || e.getCode() >= 500)
                .map(ErrorCode::getCode)
                .distinct()
                .count();
        long nonHttpCount = java.util.Arrays.stream(ErrorCode.values())
                .filter(e -> e.getCode() < 400 || e.getCode() >= 500)
                .count();
        assertThat(distinctCount).isEqualTo(nonHttpCount);
    }
}
