package com.jinshu.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Result 统一响应封装测试")
class ResultTest {

    @Test
    @DisplayName("success()：无数据成功，code=0, message=success")
    void given_noData_when_success_then_code0MessageSuccess() {
        Result<Void> result = Result.success();

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("success(T data)：带数据成功，code=0，data 正确")
    void given_data_when_success_then_dataReturned() {
        String payload = "hello";
        Result<String> result = Result.success(payload);

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("success(T data)：复杂类型作为 data")
    void given_complexData_when_success_then_dataReturned() {
        List<String> list = List.of("a", "b", "c");
        Result<List<String>> result = Result.success(list);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).hasSize(3).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("error(code, message)：带错误码的错误响应")
    void given_errorCodeAndMessage_when_error_then_fieldsMatch() {
        Result<Void> result = Result.error(400, "参数错误");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数错误");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("error(message)：仅消息，默认 code=500")
    void given_messageOnly_when_error_then_defaultCode500() {
        Result<Void> result = Result.error("服务器内部错误");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("服务器内部错误");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("success() 与 error() 返回的 code 不同")
    void given_successAndError_when_compareCode_then_different() {
        Result<Void> ok = Result.success();
        Result<Void> err = Result.error("出错");

        assertThat(ok.getCode()).isNotEqualTo(err.getCode());
    }
}
