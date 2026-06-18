package com.jinshu.common.exception;

import com.jinshu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * 统一处理系统所有异常，返回标准Result格式
 *
 * 处理类型：
 * - BusinessException: 业务异常
 * - MethodArgumentNotValidException: 参数校验异常
 * - BindException: 参数绑定异常
 * - Exception: 通用异常兜底
 *
 * 特点：
 * - 自动记录日志
 * - 统一返回格式
 * - 区分异常类型返回不同错误码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e 参数绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数绑定失败";
        log.warn("参数绑定异常: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理限流异常
     *
     * 返回 HTTP 429 Too Many Requests，并附带标准限流响应头：
     * - Retry-After
     * - X-RateLimit-Limit
     * - X-RateLimit-Window
     *
     * @param e 限流异常
     * @return 429 响应
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleRateLimitException(RateLimitException e) {
        log.warn("触发限流: code={}, limit={}, window={}s, retryAfter={}s",
                e.getCode(), e.getLimit(), e.getWindowSeconds(), e.getRetryAfterSeconds());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        headers.add("X-RateLimit-Limit", String.valueOf(e.getLimit()));
        headers.add("X-RateLimit-Window", e.getWindowSeconds() + "s");

        Map<String, Object> data = new HashMap<>();
        data.put("retryAfter", e.getRetryAfterSeconds());
        data.put("limit", e.getLimit());
        data.put("window", e.getWindowSeconds() + "s");

        Result<Map<String, Object>> body = Result.error(e.getCode(), e.getMessage());
        body.setData(data);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(body);
    }

    /**
     * 处理通用异常（兜底）
     *
     * @param e 系统异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误");
    }
}
