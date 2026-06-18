package com.jinshu.common.exception;

import lombok.Getter;

/**
 * 限流异常
 *
 * 触发 API 速率限制或租户并发配额时抛出，
 * 由全局异常处理器转换为 HTTP 429 响应并携带限流头信息。
 */
@Getter
public class RateLimitException extends BusinessException {

    /**
     * 限流阈值
     */
    private final int limit;

    /**
     * 限流窗口时长（秒）
     */
    private final int windowSeconds;

    /**
     * 建议客户端等待多少秒后重试
     */
    private final int retryAfterSeconds;

    /**
     * 构造限流异常
     *
     * @param errorCode         错误码枚举
     * @param limit             限流阈值
     * @param windowSeconds     限流窗口时长（秒）
     * @param retryAfterSeconds 建议重试等待秒数
     */
    public RateLimitException(ErrorCode errorCode, int limit, int windowSeconds, int retryAfterSeconds) {
        super(errorCode);
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
