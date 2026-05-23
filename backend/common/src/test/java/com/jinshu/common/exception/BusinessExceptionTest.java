package com.jinshu.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException 业务异常测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException(String)：自定义消息，code 为默认 500")
    void given_customMessage_when_construct_then_code500() {
        BusinessException ex = new BusinessException("自定义错误");

        assertThat(ex.getMessage()).isEqualTo("自定义错误");
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
    }

    @Test
    @DisplayName("BusinessException(ErrorCode)：使用枚举，code 和 message 自动匹配")
    void given_errorCodeEnum_when_construct_then_codeAndMessageMatch() {
        BusinessException ex = new BusinessException(ErrorCode.REPORT_NOT_FOUND);

        assertThat(ex.getCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND.getCode());
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.REPORT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("BusinessException(ErrorCode, String)：枚举 + 自定义消息")
    void given_errorCodeAndCustomMessage_when_construct_then_customMessage() {
        BusinessException ex = new BusinessException(ErrorCode.FORBIDDEN, "无权访问此报表");

        assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
        assertThat(ex.getMessage()).isEqualTo("无权访问此报表");
    }

    @Test
    @DisplayName("BusinessException 是 RuntimeException 子类")
    void given_businessException_when_checkType_then_isRuntimeException() {
        assertThat(new BusinessException("test")).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BusinessException 可被捕获并获取错误码")
    void given_businessException_when_catch_then_getCode() {
        try {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        } catch (BusinessException e) {
            assertThat(e.getCode()).isEqualTo(3001);
            assertThat(e.getMessage()).isEqualTo("任务不存在");
        }
    }
}
