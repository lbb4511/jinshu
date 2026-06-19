package com.jinshu.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * 统一定义系统所有错误码
 *
 * 码段规划：
 * - 0-99: 通用成功/错误
 * - 400-499: HTTP标准状态码映射
 * - 500-599: 服务器错误
 * - 1000-1999: 租户/用户相关
 * - 2000-2999: 报表相关
 * - 3000-3999: 任务相关
 * - 4000-4999: 文件相关
 * - 5000-5999: 数据源相关
 */
@Getter
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限"),

    /**
     * 当前角色无权限执行此操作
     */
    ROLE_NOT_ALLOWED(20001, "当前角色无权限执行此操作"),

    /**
     * 仅资源所有者可执行此操作
     */
    NOT_RESOURCE_OWNER(20002, "仅资源所有者可执行此操作"),

    /**
     * 仅指派的审查人可审批此报表
     */
    NOT_ASSIGNED_REVIEWER(20003, "仅指派的审查人可审批此报表"),

    /**
     * 不允许审查自己创建的报表
     */
    SELF_REVIEW_NOT_ALLOWED(20004, "不允许审查自己创建的报表"),

    /**
     * 跨租户访问被拒绝
     */
    CROSS_TENANT_DENIED(20005, "跨租户访问被拒绝"),

    /**
     * 目标用户已被禁用
     */
    TARGET_USER_DISABLED(20006, "目标用户已被禁用"),

    /**
     * 无效的角色授予
     */
    INVALID_ROLE_GRANT(20007, "目标用户不能授予此角色"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 系统内部错误
     */
    INTERNAL_ERROR(500, "系统内部错误"),

    /**
     * 租户不存在
     */
    TENANT_NOT_FOUND(1001, "租户不存在"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1002, "用户不存在"),

    /**
     * 用户名或密码错误
     */
    USERNAME_PASSWORD_ERROR(1003, "用户名或密码错误"),

    /**
     * 报表不存在
     */
    REPORT_NOT_FOUND(2001, "报表不存在"),

    /**
     * 报表模板不存在
     */
    REPORT_TEMPLATE_NOT_FOUND(2002, "报表模板不存在"),

    /**
     * 任务不存在
     */
    TASK_NOT_FOUND(3001, "任务不存在"),

    /**
     * 任务状态不允许此操作
     */
    TASK_STATUS_ERROR(3002, "任务状态不允许此操作"),

    /**
     * 文件类型不支持
     */
    FILE_TYPE_NOT_SUPPORTED(4001, "文件类型不支持"),

    /**
     * 文件大小超出限制
     */
    FILE_SIZE_EXCEEDED(4002, "文件大小超出限制"),

    /**
     * 磁盘空间不足
     */
    DISK_SPACE_INSUFFICIENT(4003, "存储空间不足"),

    /**
     * 文件表头与报表 schema 不匹配
     */
    COLUMN_MISMATCH(2008, "文件表头与报表 schema 不匹配"),

    /**
     * 数据源连接失败
     */
    DATA_SOURCE_CONNECT_FAILED(5001, "数据源连接失败"),

    /**
     * 账号已锁定
     */
    ACCOUNT_LOCKED(1004, "账号已锁定，请15分钟后重试"),

    /**
     * 用户已禁用
     */
    USER_DISABLED(1005, "用户已被禁用，请联系管理员"),

    /**
     * 租户已禁用
     */
    TENANT_DISABLED(1006, "租户已被禁用"),

    /**
     * Token已过期
     */
    TOKEN_EXPIRED(401, "Token已过期"),

    /**
     * Token无效
     */
    TOKEN_INVALID(401, "Token无效"),

    /**
     * Refresh Token已失效
     */
    REFRESH_TOKEN_INVALID(401, "Refresh Token已失效，请重新登录"),

    /**
     * PDF 渲染相关错误
     */
    PDF_PAGE_LIMIT_EXCEEDED(3003, "报表页数超出限制，最大支持 500 页"),
    PDF_RENDER_FAILED(3004, "PDF 渲染失败"),
    PDF_MERGE_FAILED(3005, "PDF 分片合并失败"),
    PDF_TASK_TIMEOUT(3006, "PDF 渲染超时"),

    /**
     * 请求过于频繁，触发限流
     */
    RATE_LIMIT_EXCEEDED(42900, "请求过于频繁，请稍后重试"),

    /**
     * 租户并发请求数超过配额
     */
    TENANT_CONCURRENCY_EXCEEDED(42901, "租户并发请求数超过配额，请稍后重试"),

    /**
     * 脱敏规则不存在
     */
    DESENSITIZE_RULE_NOT_FOUND(6001, "脱敏规则不存在"),

    /**
     * 脱敏规则已存在
     */
    DESENSITIZE_RULE_CONFLICT(6002, "脱敏规则已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
