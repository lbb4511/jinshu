package com.jinshu.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页查询结果封装类
 *
 * 用于统一分页查询的返回格式
 *
 * 使用示例：
 * <pre>{@code
 * // 构造分页结果
 * PageResult<User> pageResult = PageResult.of(userList, totalCount, pageNum, pageSize);
 *
 * // 包装到Result返回
 * return Result.success(pageResult);
 * }</pre>
 */
@Data
public class PageResult<T> {

    /**
     * 当前页数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从1开始）
     */
    private int pageNum;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 构造函数
     *
     * @param list 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     */
    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 静态工厂方法创建PageResult
     *
     * @param list 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     * @param <T> 数据类型
     * @return PageResult对象
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
