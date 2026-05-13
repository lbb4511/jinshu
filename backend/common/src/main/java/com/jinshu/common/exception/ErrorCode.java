package com.jinshu.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    TENANT_NOT_FOUND(1001, "租户不存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    USERNAME_PASSWORD_ERROR(1003, "用户名或密码错误"),
    REPORT_NOT_FOUND(2001, "报表不存在"),
    TASK_NOT_FOUND(3001, "任务不存在"),
    TASK_STATUS_ERROR(3002, "任务状态不允许此操作"),
    FILE_TYPE_NOT_SUPPORTED(4001, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(4002, "文件大小超出限制"),
    DATA_SOURCE_CONNECT_FAILED(5001, "数据源连接失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
