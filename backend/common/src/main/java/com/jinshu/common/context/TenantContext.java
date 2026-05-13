package com.jinshu.common.context;

/**
 * 多租户上下文工具类
 *
 * 基于ThreadLocal存储当前请求的租户ID
 *
 * 使用场景：
 * - 请求拦截器从token或请求头提取租户ID
 * - MyBatis拦截器自动注入tenant_id条件
 * - 业务层获取当前租户信息
 *
 * 注意事项：
 * - 必须在请求结束时调用clear()防止内存泄漏
 * - 通常通过拦截器自动清理
 */
public class TenantContext {

    /**
     * ThreadLocal存储租户ID
     * 线程隔离，每个线程有独立的副本
     */
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private TenantContext() {
    }

    /**
     * 设置当前线程的租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取当前线程的租户ID
     *
     * @return 租户ID，可能为null
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清除当前线程的租户ID
     *
     * 防止内存泄漏，必须在请求结束时调用
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
