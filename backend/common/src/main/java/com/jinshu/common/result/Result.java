package com.jinshu.common.result;

import lombok.Data;

/**
 * 统一API响应结果封装类
 *
 * 系统所有API接口统一返回此格式
 *
 * 格式规范：
 * - code: 0表示成功，其他表示失败
 * - message: 描述信息
 * - data: 响应数据
 *
 * 使用示例：
 * <pre>{@code
 * // 成功返回数据
 * return Result.success(user);
 *
 * // 成功不返回数据
 * return Result.success();
 *
 * // 返回错误
 * return Result.error("操作失败");
 * return Result.error(ErrorCode.PARAM_ERROR.getCode(), "参数错误");
 * }</pre>
 */
@Data
public class Result<T> {

    /**
     * 状态码：0-成功，其他-失败
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 私有构造函数，禁止直接实例化
     */
    private Result() {
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        return result;
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 错误响应（指定错误码）
     *
     * @param code 错误码
     * @param message 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 错误响应（使用默认500错误码）
     *
     * @param message 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
