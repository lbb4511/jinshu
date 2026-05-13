package com.jinshu.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * 用于业务逻辑错误的统一异常处理
 *
 * 使用场景：
 * - 参数校验失败
 * - 业务规则不满足
 * - 状态流转错误
 * - 资源不存在
 *
 * 特点：
 * - 包含错误码，便于前端国际化处理
 * - 继承RuntimeException，无需强制捕获
 * - 由GlobalExceptionHandler统一处理返回
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数 - 使用默认错误码
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR.getCode();
    }

    /**
     * 构造函数 - 使用枚举错误码
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数 - 使用枚举错误码，自定义消息
     *
     * @param errorCode 错误码枚举
     * @param message 自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
